"""生成《榄园知行》智能体开发大赛项目报告（DOCX）。

不依赖第三方库，直接生成 Office Open XML，便于在受限环境中复现文档。
"""
from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from xml.sax.saxutils import escape
import zipfile


OUT = Path(__file__).with_name("榄园知行——智能体开发大赛项目报告.docx")
W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


def x(value: object) -> str:
    return escape(str(value), {'"': '&quot;'})


def rpr(bold=False, size=None, color=None, font=None, italic=False) -> str:
    parts = ["<w:rPr>"]
    if bold:
        parts.append("<w:b/><w:bCs/>")
    if italic:
        parts.append("<w:i/><w:iCs/>")
    if size:
        parts.append(f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>')
    if color:
        parts.append(f'<w:color w:val="{color}"/>')
    if font:
        parts.append(f'<w:rFonts w:ascii="{font}" w:hAnsi="{font}" w:eastAsia="{font}"/>')
    parts.append("</w:rPr>")
    return "".join(parts)


def run(text: str, **kwargs) -> str:
    if text is None:
        text = ""
    props = rpr(**kwargs)
    pieces = str(text).split("\n")
    output = []
    for index, piece in enumerate(pieces):
        if index:
            output.append("<w:r><w:br/></w:r>")
        output.append(f'<w:r>{props}<w:t xml:space="preserve">{x(piece)}</w:t></w:r>')
    return "".join(output)


def p(text: str = "", style="Normal", align=None, first_line=True, before=0,
      after=120, keep=False, page_before=False, bold=False, size=None,
      color=None, font=None, italic=False, border=False) -> str:
    prop = ["<w:pPr>", f'<w:pStyle w:val="{style}"/>']
    if align:
        prop.append(f'<w:jc w:val="{align}"/>')
    if first_line:
        prop.append('<w:ind w:firstLine="420"/>')
    prop.append(f'<w:spacing w:before="{before}" w:after="{after}" w:line="360" w:lineRule="auto"/>')
    if keep:
        prop.append("<w:keepNext/>")
    if page_before:
        prop.append("<w:pageBreakBefore/>")
    if border:
        prop.append('<w:pBdr><w:bottom w:val="single" w:sz="6" w:space="4" w:color="4472C4"/></w:pBdr>')
    prop.append("</w:pPr>")
    return "<w:p>" + "".join(prop) + run(text, bold=bold, size=size, color=color, font=font, italic=italic) + "</w:p>"


def page_break() -> str:
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def caption(text: str) -> str:
    return p(text, style="Caption", align="center", first_line=False, before=100, after=180)


def table(headers: list[str], rows: list[list[str]], widths: list[int] | None = None,
          header_color="4472C4", font_size=18) -> str:
    columns = len(headers)
    if widths is None:
        widths = [9000 // columns] * columns
    grid = "".join(f'<w:gridCol w:w="{width}"/>' for width in widths)
    result = [
        '<w:tbl><w:tblPr><w:tblW w:w="0" w:type="auto"/>'
        '<w:tblBorders><w:top w:val="single" w:sz="8" w:color="9EADBD"/>'
        '<w:left w:val="single" w:sz="8" w:color="9EADBD"/>'
        '<w:bottom w:val="single" w:sz="8" w:color="9EADBD"/>'
        '<w:right w:val="single" w:sz="8" w:color="9EADBD"/>'
        '<w:insideH w:val="single" w:sz="4" w:color="C7D4E2"/>'
        '<w:insideV w:val="single" w:sz="4" w:color="C7D4E2"/></w:tblBorders>'
        '<w:tblCellMar><w:top w:w="90" w:type="dxa"/><w:left w:w="110" w:type="dxa"/>'
        '<w:bottom w:w="90" w:type="dxa"/><w:right w:w="110" w:type="dxa"/></w:tblCellMar>'
        '</w:tblPr><w:tblGrid>' + grid + '</w:tblGrid>'
    ]

    def cell(value: str, width: int, is_head=False, index=0) -> str:
        fill = header_color if is_head else ("F3F7FB" if index % 2 == 0 else "FFFFFF")
        txt = p(value, style="TableText", align="center" if is_head else None,
                first_line=False, before=0, after=0, bold=is_head,
                color="FFFFFF" if is_head else None, size=font_size)
        return (f'<w:tc><w:tcPr><w:tcW w:w="{width}" w:type="dxa"/>'
                f'<w:shd w:val="clear" w:color="auto" w:fill="{fill}"/>'
                '<w:vAlign w:val="center"/></w:tcPr>' + txt + '</w:tc>')

    result.append('<w:tr><w:trPr><w:cantSplit/></w:trPr>' +
                  ''.join(cell(value, widths[i], True, 0) for i, value in enumerate(headers)) + '</w:tr>')
    for row_index, row in enumerate(rows):
        values = row + [""] * (columns - len(row))
        result.append('<w:tr><w:trPr><w:cantSplit/></w:trPr>' +
                      ''.join(cell(values[i], widths[i], False, row_index) for i in range(columns)) + '</w:tr>')
    result.append('</w:tbl>')
    return ''.join(result)


def diagram(rows: list[list[str]], widths: list[int], label: str) -> str:
    body = table(["系统层级" for _ in widths], rows, widths, header_color="5B9BD5", font_size=17)
    return body + caption(label)


def heading1(text: str) -> str:
    return p(text, style="Heading1", first_line=False, before=280, after=170, keep=True, border=True)


def heading2(text: str) -> str:
    return p(text, style="Heading2", first_line=False, before=210, after=120, keep=True)


def heading3(text: str) -> str:
    return p(text, style="Heading3", first_line=False, before=160, after=80, keep=True)


def toc_field() -> str:
    return (
        '<w:p><w:pPr><w:pStyle w:val="TOCHeading"/><w:jc w:val="center"/>'
        '<w:spacing w:before="0" w:after="180"/></w:pPr>'
        + run("目  录", bold=True, size=32, font="黑体") + '</w:p>'
        '<w:p><w:pPr><w:spacing w:after="80"/></w:pPr>'
        '<w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
        '<w:r><w:instrText xml:space="preserve"> TOC \\o "1-3" \\h \\z \\u </w:instrText></w:r>'
        '<w:r><w:fldChar w:fldCharType="separate"/></w:r>'
        + run("打开 Word 后右键本目录并选择“更新域”，即可生成页码目录。", color="666666", italic=True)
        + '<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>'
    )


def styles_xml() -> str:
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="{W}">
  <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/><w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="正文"/><w:qFormat/><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/><w:sz w:val="21"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="报告标题"/><w:basedOn w:val="Normal"/><w:qFormat/><w:pPr><w:jc w:val="center"/><w:spacing w:before="480" w:after="360"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体" w:ascii="Arial" w:hAnsi="Arial"/><w:b/><w:sz w:val="44"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="报告副标题"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:sz w:val="30"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="标题 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:outlineLvl w:val="0"/><w:keepNext/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="32"/><w:color w:val="1F4E79"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="标题 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:outlineLvl w:val="1"/><w:keepNext/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="28"/><w:color w:val="2F5597"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="标题 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:outlineLvl w:val="2"/><w:keepNext/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="24"/><w:color w:val="3F6D95"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Caption"><w:name w:val="题注"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:rFonts w:eastAsia="宋体"/><w:sz w:val="18"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="TableText"><w:name w:val="表格文字"/><w:basedOn w:val="Normal"/><w:rPr><w:rFonts w:eastAsia="宋体"/><w:sz w:val="18"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Code"><w:name w:val="代码"/><w:basedOn w:val="Normal"/><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="等线"/><w:sz w:val="17"/><w:color w:val="1F1F1F"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="TOCHeading"><w:name w:val="目录标题"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="32"/></w:rPr></w:style>
</w:styles>'''


def content_types() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
<Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
<Override PartName="/word/fontTable.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml"/>
<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>'''


def build_body() -> str:
    b: list[str] = []
    # Cover
    b.append(p("榄园知行", style="Title", first_line=False, before=1200, after=100))
    b.append(p("——面向校园橄榄实训果园的智能体管理与农事决策系统", style="Subtitle", first_line=False, after=520))
    b.append(p("智能体开发大赛项目报告", style="Subtitle", first_line=False, after=700, bold=True, size=34))
    b.append(p("参赛类别：智能体开发大赛", align="center", first_line=False, before=120, after=120, size=24))
    b.append(p("项目团队：____________________", align="center", first_line=False, before=120, after=120, size=24))
    b.append(p("指导教师：____________________", align="center", first_line=False, before=120, after=120, size=24))
    b.append(p("编制日期：2026 年 7 月 28 日", align="center", first_line=False, before=500, after=120, size=24))
    b.append(page_break())

    # Abstract
    b.append(p("摘  要", style="Heading1", align="center", first_line=False, before=0, after=220))
    b.append(p("农业院校橄榄实训果园兼具教学、生产示范与科研观察功能，但现有栽培规程、气象信息和历年实训记录分散在教材、文档和人工台账中，学生难以在现场快速获得与具体果园、物候期相匹配的可执行建议。针对这一问题，本文设计并实现“榄园知行”橄榄果园智能管理与农事决策系统。系统以校园果园档案为业务锚点，采用 Spring Boot、Vue 3 与 PostgreSQL/pgvector 构建 Web 管理底座，并以 LangChain4j 编排大语言模型、检索增强生成（RAG）和天气、农资用量计算等可验证工具。系统将用户问题、果园档案、人工确认物候期、知识库片段及工具结果组合为受约束的智能体上下文；通过文档解析、分块、向量化、元数据过滤和来源回传，实现可追溯的专业知识问答；通过异步任务生成机制，将自然语言建议转化为可审核、可执行、可记录的结构化农事任务。", before=0, after=140))
    b.append(p("在安全设计上，系统对农药、施肥和危险天气等高风险场景设置了提示词约束、工具失败显式反馈、权限控制和操作日志，避免把模型输出直接等同于农业诊断或处方。本文还给出项目的需求、架构、代码组织、核心实现、测试结论和后续优化方向。该系统为农业实训场景提供了“知识查询—决策辅助—任务落实—记录沉淀”的智能体闭环，可作为垂直领域智能体工程化落地的参考。", before=0, after=160))
    b.append(p("关键词：橄榄实训果园；智能体；检索增强生成；LangChain4j；农事决策；pgvector", first_line=False, bold=True, before=80, after=280))
    b.append(p("Abstract", style="Heading1", align="center", first_line=False, before=80, after=180))
    b.append(p("Olive Orchard Intelligence is an agent-based management and farming-decision system for campus olive training orchards. It combines a Spring Boot and Vue web platform with LangChain4j orchestration, retrieval-augmented generation, weather inquiry, and deterministic agricultural calculators. Orchard profiles and manually confirmed phenology are injected into every conversation; knowledge sources and tool results are returned with the answer, while generated tasks are stored as auditable structured records. The system therefore turns fragmented teaching materials and field records into a closed loop of knowledge retrieval, decision support, task execution, and training documentation. Safety boundaries, role-based authorization, streaming feedback, and invocation logs are included to make the solution suitable for an intelligent-agent competition and for supervised agricultural training use.", first_line=False, after=120, font="Times New Roman"))
    b.append(p("Key words: agricultural agent; retrieval-augmented generation; orchard management; LangChain4j; pgvector", first_line=False, bold=True, font="Times New Roman", after=160))
    b.append(page_break())
    b.append(toc_field())
    b.append(page_break())

    # 1
    b.append(heading1("1  项目背景与问题定义"))
    b.append(heading2("1.1  行业与教学背景"))
    b.append(p("校园橄榄实训果园是学生掌握巡园、灌溉、施肥、修剪、病虫害观察与采收技能的重要场所，也承担生产示范与科研观察任务。实际教学中，学生需要同时理解当前物候期、近期天气、园区条件和技术规程；教师则需要组织任务、审核结果并沉淀过程材料。传统方式主要依赖纸质教材、分散的电子资料和口头指导，现场查询效率低，且难以保证不同学生获得建议的一致性与可追溯性。"))
    b.append(p("通用对话模型虽然具备自然语言交互能力，却不了解学校果园的面积、株数、品种、灌溉方式和已确认物候期；在涉及时效性天气、农药剂量和用量计算时，还可能出现无来源回答或数值失真。因此，本项目不把大模型定位为独立的“专家替代者”，而是将其置于校本知识、结构化档案、确定性工具和教师审核共同约束的工作流中。"))
    b.append(heading2("1.2  问题界定"))
    b.append(table(["问题维度", "现有痛点", "系统应对"], [
        ["知识获取", "技术规程、实训指导书与历史经验分散，现场检索耗时。", "上传并解析 PDF、DOCX、TXT、Markdown，按语义检索并回传资料来源。"],
        ["情境匹配", "通用知识难以对应具体果园、物候期和作业条件。", "将果园档案和人工确认物候期写入 Agent 上下文。"],
        ["实时判断", "天气变化影响灌溉、施肥和作业安全，人工查询链路长。", "Agent 按需调用天气工具，区分实时、缓存与演示数据。"],
        ["数量计算", "按株数、单位或稀释倍数手算易出错。", "将灌溉、肥料、药剂稀释、产量估算封装为 BigDecimal 工具。"],
        ["教学闭环", "建议、任务、执行和记录割裂，教师难以回溯。", "将建议转为结构化农事任务，并保存对话、工具调用和实训记录。"],
        ["风险控制", "模型可能虚构资料、天气或高风险操作结论。", "来源引用、失败显式反馈、安全提示、权限与审计共同约束。"],
    ], [1650, 3500, 3850]))
    b.append(caption("表 1-1  项目问题与应对关系"))
    b.append(heading2("1.3  建设目标与参赛定位"))
    b.append(p("项目面向智能体开发大赛，核心目标是展示一个能够完成“感知上下文、检索知识、选择工具、生成可执行结果、留下可审计过程”的垂直领域智能体，而非单纯的聊天页面。参赛系统需要完成以下闭环：其一，建立中国橄榄专业知识库；其二，维护校内果园与物候期数字档案；其三，结合实时或明确标识的天气信息给出建议；其四，自动选择确定性计算工具并返回精确结果；其五，生成结构化农事任务和实训记录；其六，以权限、来源、日志和安全边界保证可演示、可验证、可追溯。"))
    b.append(heading2("1.4  项目创新点"))
    b.append(p("（1）面向实体果园的上下文智能体：不只接收问题文本，而是将果园 ID、面积、株数、品种、灌溉方式和人工确认物候期注入系统提示词，使回答具备明确的适用边界。"))
    b.append(p("（2）知识与工具协同的可验证决策：RAG 负责提供校本专业依据，天气与计算工具负责提供外部事实和精确数值，模型负责解释、组织与追问，降低纯生成式回答的幻觉风险。"))
    b.append(p("（3）从对话到任务的业务闭环：将模型生成的建议进一步校验并持久化为农事任务，支持异步生成、状态查询、教师审核和学生执行，而不是止于一次性文本输出。"))
    b.append(p("（4）内建农业安全约束：针对农药、施肥、修剪、雷雨和高温等风险场景，将“资料依据、产品标签核对、教师确认、不得冒险作业”等规则写入提示词、工具结果与前端反馈。"))

    # 2
    b.append(heading1("2  需求分析"))
    b.append(heading2("2.1  用户角色与权限边界"))
    b.append(table(["角色", "核心任务", "权限边界"], [
        ["学生", "查询果园信息、发起智能问答、使用计算器、领取并反馈任务、提交实训记录。", "仅能访问本人会话和本人创建的记录；不能维护用户、知识库或果园基础档案。"],
        ["教师/管理员", "维护果园与物候期，上传知识资料，生成与审核任务，查看全体实训记录。", "拥有学生权限并可进行教学管理；高风险建议仍需人工确认。"],
        ["系统管理员", "维护账号、模型配置状态和系统运行情况。", "系统首版将教师/管理员与系统管理员能力按 ADMIN 角色集中实现。"],
    ], [1600, 4000, 3400]))
    b.append(caption("表 2-1  用户角色与权限分析"))
    b.append(heading2("2.2  功能需求"))
    b.append(table(["功能域", "关键需求", "验收要点"], [
        ["认证与权限", "用户名密码登录、JWT 鉴权、管理员与学生角色控制。", "密码不明文保存；后端拒绝越权访问。"],
        ["果园档案", "维护果园面积、树龄、株数、品种、灌溉方式与物候期历史。", "Agent 使用人工确认的当前物候期，不擅自覆盖。"],
        ["智能问答", "多轮对话、流式输出、果园上下文、引用来源和工具状态。", "天气与计算类问题能触发对应工具；工具失败不编造结果。"],
        ["知识库", "资料上传、类型校验、解析、切分、向量化、检索与处理状态查询。", "检索结果包含资料名、片段、页码或片段编号和相似度。"],
        ["农事任务", "按果园和日期异步生成结构化任务，支持查询、状态流转和执行反馈。", "任务包括优先级、依据、建议时段和安全提示。"],
        ["农业计算", "灌溉、肥料、药剂稀释、产量估算。", "公式、输入、单位换算和安全提示可追溯。"],
        ["实训记录", "关联任务、学生、现象、措施与教师评语，并支持文件材料。", "记录具有提交人和时间，教师可查询与评价。"],
    ], [1500, 4250, 3250]))
    b.append(caption("表 2-2  核心功能需求"))
    b.append(heading2("2.3  智能体业务流程"))
    b.append(diagram([
        ["① 用户选择果园并提问", "→", "② 身份与会话校验", "→", "③ 注入果园/物候期上下文"],
        ["④ RAG 检索校本资料", "→", "⑤ Agent 判断并调用工具", "→", "⑥ 模型组织建议与安全提示"],
        ["⑦ 前端流式展示答案、引用、工具状态", "→", "⑧ 保存消息、调用日志与来源", "→", "⑨ 可转化为农事任务/实训记录"],
    ], [2500, 450, 2500, 450, 2900], "图 2-1  智能问答与农事决策闭环"))
    b.append(heading2("2.4  非功能需求"))
    b.append(p("性能方面，普通 CRUD 接口应保持快速响应，RAG 检索不应被模型生成时间阻塞，长文本回答采用 SSE 流式返回以缩短用户等待感。稳定性方面，模型、嵌入模型和天气 API 均应配置超时、有限重试和明确错误码；任务生成使用后台线程和可轮询批次，避免 HTTP 请求长时间占用。安全性方面，密钥仅从环境变量读取，密码使用 BCrypt 摘要，接口使用 JWT 与角色校验，上传资料限制文件大小、扩展名和 MIME 类型。可维护性方面，后端以业务域分包，数据库通过 Flyway 管理，前后端接口统一以 /api/v1 为前缀并提供 Swagger 文档。"))

    # 3
    b.append(heading1("3  系统架构设计"))
    b.append(heading2("3.1  总体分层架构"))
    b.append(diagram([
        ["交互层", "Vue 3 + TypeScript + Vite\nElement Plus、Pinia、Vue Router\n登录、总览、问答、任务、知识库、记录"],
        ["接口与安全层", "Spring MVC REST / SSE\nApiResponse、参数校验、RequestId、CORS\nJWT、BCrypt、RBAC、Swagger"],
        ["智能体编排层", "LangChain4j Agent\n系统提示词 + 会话记忆 + RAG ContentRetriever\n天气/果园/计算工具 + 调用次数限制 + 工具日志"],
        ["业务服务层", "果园与物候期｜知识库处理｜聊天会话｜农事任务｜实训记录｜用户管理｜天气服务"],
        ["数据与外部层", "H2（本地）/ PostgreSQL + pgvector（部署）\nFlyway、JPA、文件存储\n阿里云百炼兼容 API、高德天气 API"],
    ], [1700, 7300], "图 3-1  系统总体分层架构"))
    b.append(p("前端负责将复杂业务过程呈现为页面和流式交互；后端 Controller 保持协议转换职责，业务逻辑下沉至各领域 Service；Agent 层作为编排核心，但不直接绕过权限、数据访问和工具服务。该分层确保模型供应商、数据库形态或前端组件发生变化时，能够在相对局部的模块中替换。"))
    b.append(heading2("3.2  智能体运行架构"))
    b.append(diagram([
        ["用户消息", "会话归属校验", "PromptFactory 注入果园上下文", "LangChain4j 对话服务"],
        ["知识库检索", "RagContentRetriever：TopK=5，最低相似度=0.55", "工具选择", "天气｜果园｜灌溉｜肥料｜稀释｜产量"],
        ["来源片段", "文档名、来源单位、片段 ID、页码、得分", "结果与日志", "SSE token / tool_call / tool_result / done"],
    ], [1500, 2450, 2200, 2850], "图 3-2  Agent 检索、工具调用与流式反馈结构"))
    b.append(p("每次对话均使用会话 ID 作为记忆隔离键。系统提示词明确要求：涉及天气、降雨、温度、风力或未来日期时调用天气工具；涉及数量、单位换算和稀释倍数时调用计算工具；无可靠检索来源或工具失败时必须明确说明。工具执行入口统一记录输入、输出状态、耗时和异常摘要，并通过会话上下文限制调用轮次，避免无限工具循环。"))
    b.append(heading2("3.3  数据架构"))
    b.append(table(["数据域", "主要实体", "设计目的"], [
        ["身份与权限", "app_user、operation_log", "保存用户、角色、状态及关键操作轨迹。"],
        ["果园业务", "orchard、phenology_record、farming_task、training_record", "表达园区静态档案、物候期历史、任务状态与教学执行结果。"],
        ["对话与审计", "chat_session、chat_message、tool_call_log、model_call_log", "保存会话归属、模型回答、引用、工具调用与模型调用概要。"],
        ["知识库", "knowledge_document、knowledge_chunk", "保存资料元数据、处理状态、文本片段、向量、页码和过滤标签。"],
        ["异步任务", "task_generation_job", "保存生成批次、状态、天气摘要、来源和失败信息，支持轮询与失败重试。"],
    ], [1700, 3400, 3600]))
    b.append(caption("表 3-1  核心数据实体与职责"))
    b.append(p("本地开发默认使用 H2 文件数据库，并在 PostgreSQL 兼容模式下执行公共迁移；容器部署环境启用 PostgreSQL 和 pgvector 扩展。RAG 模块会运行时识别数据库类型：PostgreSQL 场景使用向量列和余弦距离算子进行数据库侧检索；H2 场景保留编码后的向量，并使用 Java 余弦相似度作为开发回退方案，从而兼顾现场部署性能与本地可用性。"))
    b.append(heading2("3.4  部署架构"))
    b.append(p("项目提供 Docker Compose 编排。frontend 通过 Nginx 提供静态页面；backend 以 Spring Boot 容器运行；postgres 使用预置 vector 扩展的 pgvector/pgvector:pg16 镜像并通过健康检查控制依赖启动顺序。模型 API Key、数据库密码和演示账号密码均由 .env 或部署环境变量注入，不进入源代码与前端资源。开发环境仍可分别启动后端与 Vite 前端，以支持接口调试和热更新。"))

    # 4
    b.append(heading1("4  核心技术实现细节"))
    b.append(heading2("4.1  果园上下文与安全提示词"))
    b.append(p("AgentPromptFactory 在每次会话开始前查询目标果园，并生成包含果园名称、地区、面积、株数、品种、灌溉方式、人工确认物候期及生效日期的系统提示词。提示词同时规定工具选择规则、来源展示规则和农业安全边界。由于物候期由管理员人工维护，模型只能读取而不能覆盖，这避免了模型根据泛化知识误判本地生产阶段。"))
    b.append(p("安全约束不是单一的免责声明，而是由三层共同完成：提示词限制模型的表达边界；确定性工具在药剂稀释与产量估算结果中返回警告字段；界面与业务流程要求教师确认高风险操作。对“落果”“叶片异常”等现象，系统要求给出可能原因和检查项，而不做确定诊断；对农药建议，要求核对登记作物、产品标签和安全间隔期。"))
    b.append(heading2("4.2  RAG 知识库处理与混合检索"))
    b.append(p("管理员上传资料后，KnowledgeFileStorage 先校验非空、大小、文件名、扩展名与 MIME 类型，并以 UUID 文件名保存到受控目录，防止路径穿越。KnowledgeParserRegistry 根据类型选择 PDF、DOCX 或纯文本解析器。KnowledgeDocumentProcessor 在异步线程中完成解析、清洗、切块、向量化和状态更新；失败时记录有限长度的原因，文档状态可区分待处理、处理中、成功和失败。"))
    b.append(p("DocumentChunker 以页面为单位进行清洗和切分，单片最大约 800 个字符、相邻片段重叠 100 个字符，并优先在句号、问号、换行等边界断开。这一策略在保留上下文连续性的同时，使嵌入模型输入和召回片段大小保持稳定。每个片段同时保存文档 ID、片段序号、页码、地区、物候期和资料类型，支持针对性过滤。"))
    b.append(p("检索阶段先按元数据获取候选片段，再按照嵌入供应商分别生成查询向量，最终依相似度排序并应用最低阈值和 TopK 限制。PostgreSQL 环境以“1 - (embedding_vector <=> query_vector)”计算余弦相似度；H2 则用 Java 实现的余弦函数回退。RagContentRetriever 将检索结果适配为 LangChain4j Content，并将资料名、来源单位、片段 ID、页码和得分附加至元数据，供对话返回引用。"))
    b.append(heading2("4.3  工具调用与可验证计算"))
    b.append(table(["工具", "触发场景", "关键实现与输出"], [
        ["queryOrchardWeather", "天气、降雨、温度、风力、未来日期。", "查询高德天气；按果园缓存；API 未配时仅返回明确标识的 DEMO 数据；失败可返回带过期标记的历史成功缓存。"],
        ["getOrchardContext", "需要补充果园档案或物候期。", "读取果园服务，保证 Agent 获取的事实与业务页面一致。"],
        ["calculateIrrigation", "株数与单株用水量。", "treeCount × litersPerTree，返回升和立方米。"],
        ["calculateFertilizer", "株数、单株肥料用量及单位。", "统一换算为千克，并返回吨；使用 BigDecimal 和 HALF_UP。"],
        ["calculateDilution", "目标药液体积与稀释倍数。", "solutionLiters × 1000 ÷ dilutionRatio，返回原药毫升及安全提示。"],
        ["calculateYieldEstimate", "抽样株数、抽样产量、总株数。", "计算单株平均值和全园估算值，明确标记为估算而非最终产量。"],
    ], [2000, 2500, 4200]))
    b.append(caption("表 4-1  Agent 工具映射"))
    b.append(p("所有工具通过 AgentToolExecutor 统一执行。执行器在调用前进入会话级调用上下文，在调用后写入工具名、输入、结果、状态和毫秒级耗时；异常情况下也会记录 FAILED 状态后继续向上抛出。这样一来，前端能够在 SSE 通道中展示工具正在执行、成功或失败的过程，教师也可在工具日志中回溯模型做出某项建议所依据的外部查询或公式。"))
    b.append(heading2("4.4  流式多轮对话与可停止生成"))
    b.append(p("聊天模块以数据库会话保存用户与果园归属，并使用会话 ID 初始化记忆。用户消息先落库，系统创建助手消息占位记录；随后通过 LangChain4j 的 TokenStream 将增量内容推送为 SSE 事件。前端可接收 token、tool_call、tool_result、done 与 error 等事件，避免长答案生成时出现空白等待。生成完成后，系统写入模型名、结束原因、耗时和来源快照；用户主动停止时，系统保存已生成的部分文本、清理会话记忆并向客户端发出停止完成事件。"))
    b.append(heading2("4.5  异步农事任务生成"))
    b.append(p("农事任务不同于即时聊天，需要组合天气摘要、果园与物候期、RAG 资料和模型生成结果，并将结果保存为业务对象。TaskGenerationCoordinator 先创建或复用以“果园—日期”为维度的生成批次，随后发布事件。TaskGenerationWorker 在专用异步执行器中认领批次、调用生成服务、保存天气摘要、物候期和引用；异常时将批次标记为 FAILED 并记录有限长度的错误信息。前端只需轮询批次状态，不会因外部模型延迟而占用同步请求。"))
    b.append(heading2("4.6  模型配置、权限与可观测性"))
    b.append(p("BailianModelFactory 按需、线程安全地创建普通聊天、流式聊天、任务生成和嵌入模型。模型通过 OpenAI 兼容接口接入阿里云百炼，基础地址、模型名、温度、超时、重试次数和嵌入维度均由环境变量配置；API Key 为空时应用仍可启动，但首次真实模型调用会返回明确配置错误。请求和响应日志被关闭，模型调用日志仅保存平台、模型、耗时、成功状态和错误摘要，从设计上避免泄露密钥。"))
    b.append(p("安全模块使用 BCryptPasswordEncoder 保存密码摘要，JwtService 签发包含用户 ID 和角色的短期令牌，默认有效期为 7 200 秒。SecurityConfig 将登录、健康检查和 API 文档设置为公开，其余接口默认需认证，用户管理接口需 ADMIN 角色；具体业务服务还对会话、文件和实训记录进行所有者或管理员校验。数据库结构由 Flyway 版本迁移管理，避免依赖运行时自动建表。"))

    # 5
    b.append(heading1("5  代码结构与工程组织"))
    b.append(p("项目采用前后端分离的单仓库组织。后端围绕业务能力而非单纯技术层次划分包，前端以页面、组件、状态和接口类型分层；文档目录维护需求、接口、数据库与开发约定。这样的结构适合多人在智能体、业务 CRUD、前端交互和部署模块上并行协作。"))
    b.append(table(["目录/模块", "主要内容", "职责说明"], [
        ["backend/src/main/java/com/lanyuan/starter/agent", "OliveOrchardAgent、PromptFactory、AgentToolExecutor、各类 @Tool。", "封装 Agent 契约、上下文、工具与调用审计。"],
        ["…/chat、…/task", "会话、消息、SSE、记忆、任务生成批次和状态机。", "完成多轮对话与对话到任务的流程闭环。"],
        ["…/knowledge、…/rag", "上传、解析、切块、嵌入、向量检索、ContentRetriever。", "完成知识库全生命周期与来源引用。"],
        ["…/orchard、…/weather、…/calculator、…/training", "果园、物候期、天气、农业计算、实训记录。", "承载可验证的领域业务与工具能力。"],
        ["…/security、…/config、…/common", "JWT、过滤器、CORS、安全配置、统一响应、异常处理。", "提供横切能力和接口治理。"],
        ["backend/src/main/resources/db/migration", "V1 至 V103 以及 PostgreSQL 专用 pgvector 迁移。", "以版本化脚本管理数据库演进。"],
        ["frontend/src/views、components、stores、api.ts", "登录、总览、问答、任务、知识库、用户与记录页面。", "提供面向学生与管理员的 Web 交互。"],
        ["docs、docker-compose.yml", "需求、接口、数据库、开发规范与容器编排。", "支撑协作、部署与演示复现。"],
    ], [2550, 3500, 2650]))
    b.append(caption("表 5-1  代码目录与模块职责"))
    b.append(heading2("5.1  接口与数据约定"))
    b.append(p("接口统一以 /api/v1 为前缀，返回值使用 ApiResponse<T>，分页使用 PageResponse，Controller 仅承担协议转换，参数校验和异常统一处理。对外接口通过 springdoc 生成 Swagger 页面。金额、用量和单位换算使用 BigDecimal，以避免浮点误差；实体关系和状态枚举在后端集中维护，前端 types.ts 与 api.ts 统一定义调用契约。"))
    b.append(heading2("5.2  前端交互组织"))
    b.append(p("前端采用 Vue 3 Composition API、TypeScript、Vite 和 Element Plus。AppShell 提供统一导航框架；ChatView 负责流式消息、引用和工具状态展示；TasksView、RecordsView、OrchardsView 和 KnowledgeView 分别对应业务闭环中的任务、记录、档案与知识库环节；Pinia 的 auth store 管理令牌和当前用户。对富文本回答使用 marked 渲染，并引入 DOMPurify 降低 XSS 风险。"))

    # 6
    b.append(heading1("6  优化方案与演进路线"))
    b.append(heading2("6.1  检索质量优化"))
    b.append(p("当前系统已支持向量召回、最低相似度、TopK 和元数据过滤。后续可建设人工标注的问题—资料集合，将命中率、MRR、引用完整率和拒答准确率纳入离线评测；采用关键词与向量的混合召回、重排序模型、标题/章节权重及相邻片段合并，提高农业术语、地方规程和短问句场景的召回质量。对低相似度结果，应优先追问或明确拒答，而不是将边缘片段拼接成确定性结论。"))
    b.append(heading2("6.2  Agent 规划与安全优化"))
    b.append(p("可将现有提示词约束进一步工程化为显式策略层：按问题类型路由到“知识问答、天气决策、数量计算、异常排查、任务生成”工作流；高风险问题进入教师确认队列；模型输出通过 JSON Schema 校验、单位一致性校验和禁用词规则复核。对于外部工具，应设置幂等键、熔断、限流和重试退避；工具返回应包含数据源、时间戳与可信度，以便 Agent 在答案中解释证据时效性。"))
    b.append(heading2("6.3  性能与可靠性优化"))
    b.append(p("PostgreSQL 生产环境应为向量列、文档状态和常用元数据建立合适索引，并按知识库规模引入 HNSW 或 IVF 类向量索引；文档嵌入采用限并发队列、断点重试和批量请求控制成本。聊天和任务生成可引入请求超时、取消令牌与队列监控，避免单次模型服务抖动放大为线程堆积。模型、天气与数据库的关键指标应接入统一监控，包括首 token 延迟、完成时延、工具成功率、缓存命中率、任务失败率和知识库处理耗时。"))
    b.append(heading2("6.4  数据治理与应用扩展"))
    b.append(p("知识资料应增加来源等级、适用地区、发布日期、审核人和失效标记；管理员发布资料前可进行预览和抽样验收。随着实训记录积累，可在明确授权和脱敏前提下形成“问题—天气—任务—效果”的案例库，为教师复盘和模型评测提供依据。未来可接入传感器、病虫害图像辅助识别或多园区管理，但应保持“设备数据只作为证据，关键农事决策由教师确认”的边界，避免在证据不足时自动控制设备或给出处方。"))

    # 7
    b.append(heading1("7  应用价值"))
    b.append(heading2("7.1  教学价值"))
    b.append(p("系统把抽象的技术规程转化为与当前果园、物候期和天气相关的交互式问题，帮助学生在现场形成“先核对事实、再查证资料、再执行任务”的工作习惯。对话来源、工具过程和任务记录均可回溯，教师可据此评价学生的判断依据与执行质量，而不只评价最终答案。"))
    b.append(heading2("7.2  管理价值"))
    b.append(p("果园档案、物候期、任务和实训记录进入统一平台后，管理者能够减少重复沟通，快速查看未完成任务、历史现象和处理措施。异步任务批次和状态流转使每日农事安排具有可查询、可审核和可追责的载体；知识库资料的来源与版本管理则为校内技术规范的持续积累提供基础。"))
    b.append(heading2("7.3  技术示范价值"))
    b.append(p("项目展示了垂直领域智能体从原型演示走向工程系统的路径：以结构化业务数据约束模型，以 RAG 补足私有知识，以可验证工具替代模型算术和实时事实，以流式交互提升体验，以权限和日志建立治理基础。该模式可迁移到茶园、果蔬、林业、畜牧等其他实训场景，仅需替换知识库、物候期模型和领域工具。"))
    b.append(heading2("7.4  社会与风险价值"))
    b.append(p("农业智能化不应放大“黑箱建议”风险。本项目将模型定位为教师与学生的辅助工具，明确要求来源、数据时间和安全提醒，并保留人工确认环节。该设计既能降低初学者的信息获取门槛，也能在用药、施肥、极端天气等高风险环节提醒用户回到标准规程和专业人员判断，为负责任的农业智能体实践提供可操作的范式。"))

    # 8
    b.append(heading1("8  测试结果与质量评估"))
    b.append(heading2("8.1  测试环境与方法"))
    b.append(p("测试面向后端单元测试、Spring 上下文/数据库迁移测试、权限测试、Agent 工具编排测试、RAG 检索效果测试和 SSE 协议测试。项目后端基于 Java 17、Spring Boot 3.3.5、Maven Surefire 与 JUnit 5 运行；测试配置使用 H2 内存数据库并由 Flyway 初始化模式。前端使用 TypeScript 编译和 Vite 打包作为静态构建检查。外部模型与高德天气在单元测试中使用替身或演示数据，避免测试依赖真实密钥与网络波动。"))
    b.append(heading2("8.2  已覆盖测试内容"))
    b.append(table(["测试类别", "代表性验证点", "结果说明"], [
        ["Agent 工具编排", "一次问题触发天气、灌溉量和药剂稀释 3 个真实 Java 工具；验证工具结果回传及调用日志。", "测试替身验证通过；6000.00 L 灌溉量和 200.00 mL 稀释量均被断言。"],
        ["农业计算", "灌溉、肥料单位换算、稀释、产量估算及非法输入。", "CalculatorServiceTest 共 10 项，本次复测通过。"],
        ["RAG 检索", "标注问题命中预期水肥资料和病虫害资料片段。", "RagRetrievalEffectTest 已覆盖本地向量回退逻辑和预期文档命中。"],
        ["聊天流式协议", "content-type、禁缓存、连接保持、Nginx 禁缓冲头。", "ChatControllerSseTest 本次复测通过。"],
        ["权限与数据", "会话归属、果园/记录/文件权限、迁移、演示数据和接口契约。", "源码中包含对应测试类，需在迁移修复后完成全量回归。"],
    ], [1900, 4600, 2800]))
    b.append(caption("表 8-1  主要测试覆盖情况"))
    b.append(heading2("8.3  本次复测结论"))
    b.append(p("2026 年 7 月 28 日在项目 backend 目录执行“mvn --no-transfer-progress test”。在本次运行中，AgentCoreTest（2 项）、AgentToolInvocationTest（2 项）、CalculatorServiceTest（10 项）以及 ChatControllerSseTest（1 项）均通过，共 15 项测试先行通过。随后 ChatSchemaTest 启动 Spring 上下文时，Hibernate 校验发现 file_record 表缺少 uploader_id 列，测试流程因此中断。该结果表明 Agent 工具、计算和 SSE 的基础验证正常，但当前工作区版本尚不能宣称“全量回归通过”。"))
    b.append(p("同日执行前端“npm run build”通过：vue-tsc 类型检查与 Vite 生产构建均完成，共转换 1 705 个模块，构建耗时 10.94 秒。构建产物提示入口 JavaScript 压缩后约 1 122.55 kB（gzip 约 373.95 kB），超过默认 500 kB 阈值；该提示不影响本次构建通过，但应按第 6.3 节建议使用路由分包和 manualChunks 优化首屏负载。"))
    b.append(heading2("8.4  历史测试记录与差异说明"))
    b.append(p("项目 target/surefire-reports 中保留的较早测试报告显示 30 个测试类、98 项测试、0 failures、0 errors、0 skipped。这一记录可说明项目曾具备较广的测试资产与通过记录，但不应替代本次源码状态的复测结论。智能体大赛提交前，应以修复后的当前代码重新执行全量测试，并在报告和演示材料中记录对应的提交版本、环境和结果。"))
    b.append(heading2("8.5  已发现问题与修复建议"))
    b.append(p("当前复测失败的直接原因是实体映射要求 file_record.uploader_id，而现有 Flyway 迁移 V1 至 V103 未创建该列，造成测试库完成迁移后无法通过 Hibernate validate。建议新增一个高于当前版本的迁移脚本（例如 V104），以 nullable/非空策略与外键约束和 FileRecord 实体保持一致；随后执行数据库迁移测试、文件权限测试、训练记录权限测试及完整 mvn test。迁移脚本一旦发布不得回改已执行版本，以保证已有环境可平滑升级。"))
    b.append(heading2("8.6  竞赛验收建议"))
    b.append(p("建议在现场演示前完成以下验收：第一，使用管理员账号上传至少若干份经审核的橄榄资料，并核验来源引用；第二，展示一个天气问题和两个计算问题的真实工具调用；第三，展示任务生成批次从 PROCESSING 到 COMPLETED 的过程；第四，使用学生账号提交实训记录并验证越权拦截；第五，在关闭或模拟外部 API 失败时确认系统明确提示而不虚构结果；第六，修复数据库迁移问题后重新归档完整测试报告与前端构建日志。"))

    # conclusion and refs
    b.append(heading1("结  语"))
    b.append(p("“榄园知行”围绕校园橄榄实训的真实任务，完成了从校本资料管理、果园上下文建模、RAG 检索、工具调用到结构化农事任务和实训记录的智能体系统设计。其价值不在于让模型独自做出农业决策，而在于把模型的语言理解与校本知识、实时事实、精确计算和教师审核结合，形成有依据、有边界、可追溯的辅助决策过程。项目当前已具备较完整的工程框架和关键测试资产；后续应优先修复迁移一致性问题、强化检索评测与可观测性，并以真实教学资料持续验证系统效果。"))
    b.append(heading1("参考文献"))
    refs = [
        "[1] 榄园知行项目组. 榄园知行——橄榄果园智能管理与农事决策系统需求文档[Z]. 项目内部文档, 2026.",
        "[2] 榄园知行项目组. 榄园知行接口文档[Z]. 项目内部文档, 2026.",
        "[3] Spring. Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/.",
        "[4] LangChain4j. LangChain4j Documentation[EB/OL]. https://docs.langchain4j.dev/.",
        "[5] PostgreSQL Global Development Group. PostgreSQL Documentation[EB/OL]. https://www.postgresql.org/docs/.",
        "[6] pgvector. Open-source vector similarity search for Postgres[EB/OL]. https://github.com/pgvector/pgvector.",
        "[7] Vue.js. Vue.js Documentation[EB/OL]. https://vuejs.org/guide/.",
        "[8] Jones M, Bradley J, Sakimura N. JSON Web Token (JWT): RFC 7519[S]. IETF, 2015.",
    ]
    for ref in refs:
        b.append(p(ref, first_line=False, before=0, after=70, font="宋体", size=20))
    b.append(heading1("附录 A  复现与验证说明"))
    b.append(p("后端启动：进入 backend 目录后执行 mvn spring-boot:run；前端启动：进入 frontend 目录后执行 npm install 与 npm run dev；容器部署：在根目录配置 .env 后执行 docker compose up --build。后端健康状态为 /api/v1/system/status，Swagger 地址为 /swagger-ui/index.html。本报告对应的测试命令为：mvn --no-transfer-progress test。", first_line=False))
    b.append(p("演示配置中应通过环境变量提供数据库密码、JWT_SECRET、LLM_API_KEY、LLM_CHAT_MODEL、LLM_EMBEDDING_MODEL 和高德天气 API Key（如启用实时天气）。严禁将密钥写入报告、截图、前端代码或 Git 仓库。", first_line=False))
    return ''.join(b)


def create_document() -> None:
    body = build_body()
    sect = (
        '<w:sectPr><w:headerReference w:type="default" r:id="rId3"/>'
        '<w:footerReference w:type="default" r:id="rId4"/>'
        '<w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="850" w:footer="850" w:gutter="0"/>'
        '<w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>'
    )
    document = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="{W}" xmlns:r="{R}"><w:body>{body}{sect}</w:body></w:document>'''
    header = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:w="{W}"><w:p><w:pPr><w:jc w:val="right"/><w:pBdr><w:bottom w:val="single" w:sz="4" w:space="3" w:color="9EADBD"/></w:pBdr></w:pPr>{run("榄园知行——智能体开发大赛项目报告", size=18, color="666666", font="宋体")}</w:p></w:hdr>'''
    footer = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="{W}"><w:p><w:pPr><w:jc w:val="center"/></w:pPr>{run("第 ", size=18, color="666666", font="宋体")}<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText> PAGE </w:instrText></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r>{run(" 页", size=18, color="666666", font="宋体")}</w:p></w:ftr>'''
    rels = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>
<Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
<Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
</Relationships>'''
    package_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'''
    now = datetime(2026, 7, 28, tzinfo=timezone.utc).isoformat().replace('+00:00', 'Z')
    core = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>榄园知行——智能体开发大赛项目报告</dc:title><dc:subject>农业智能体；橄榄果园管理</dc:subject><dc:creator>榄园知行项目团队</dc:creator><cp:keywords>智能体,RAG,橄榄果园,农事决策</cp:keywords><dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified></cp:coreProperties>'''
    app = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft Office Word</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop><Company>榄园知行项目团队</Company><LinksUpToDate>false</LinksUpToDate><SharedDoc>false</SharedDoc><HyperlinksChanged>false</HyperlinksChanged><AppVersion>16.0000</AppVersion></Properties>'''
    settings = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:settings xmlns:w="{W}"><w:updateFields w:val="true"/><w:zoom w:percent="100"/><w:defaultTabStop w:val="420"/></w:settings>'''
    fonts = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:fonts xmlns:w="{W}"><w:font w:name="宋体"/><w:font w:name="黑体"/><w:font w:name="Times New Roman"/><w:font w:name="Consolas"/></w:fonts>'''
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as docx:
        docx.writestr("[Content_Types].xml", content_types())
        docx.writestr("_rels/.rels", package_rels)
        docx.writestr("docProps/core.xml", core)
        docx.writestr("docProps/app.xml", app)
        docx.writestr("word/document.xml", document)
        docx.writestr("word/_rels/document.xml.rels", rels)
        docx.writestr("word/styles.xml", styles_xml())
        docx.writestr("word/settings.xml", settings)
        docx.writestr("word/fontTable.xml", fonts)
        docx.writestr("word/header1.xml", header)
        docx.writestr("word/footer1.xml", footer)
    assert zipfile.is_zipfile(OUT)
    print(f"Created: {OUT}")
    print(f"Bytes: {OUT.stat().st_size}")


if __name__ == "__main__":
    create_document()
