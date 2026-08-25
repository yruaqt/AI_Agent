"""Generate an editable Word cover and a matching PNG preview for 榄园知行."""

from __future__ import annotations

from datetime import datetime, timezone
from math import comb
from pathlib import Path
from xml.sax.saxutils import escape
import zipfile

from PIL import Image, ImageDraw, ImageFont


HERE = Path(__file__).resolve().parent
DOCX_OUT = HERE / "榄园知行——项目报告封面.docx"
PREVIEW_OUT = HERE / "榄园知行——项目报告封面预览.png"
MOTIF_OUT = HERE / "榄园知行——封面橄榄智能体图案.png"

PAPER = "F7F9F5"
WHITE = "FFFFFF"
INK = "1F2924"
MUTED = "68736D"
GREEN_DARK = "16342A"
GREEN = "2E6B4E"
LIME = "A7BF68"
AMBER = "C58932"
LINE = "D8E0D9"

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("C:/Windows/Fonts/msyhbd.ttc" if bold else "C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
        Path("C:/Windows/Fonts/simsun.ttc"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


def bezier(points: list[tuple[float, float]], steps: int = 80) -> list[tuple[int, int]]:
    n = len(points) - 1
    output: list[tuple[int, int]] = []
    for step in range(steps + 1):
        t = step / steps
        x = sum(comb(n, i) * (1 - t) ** (n - i) * t**i * points[i][0] for i in range(n + 1))
        y = sum(comb(n, i) * (1 - t) ** (n - i) * t**i * points[i][1] for i in range(n + 1))
        output.append((round(x), round(y)))
    return output


def add_leaf(canvas: Image.Image, center: tuple[int, int], angle: float, scale: float, color: str) -> None:
    width = round(170 * scale)
    height = round(64 * scale)
    leaf = Image.new("RGBA", (width + 20, height + 20), (0, 0, 0, 0))
    draw = ImageDraw.Draw(leaf)
    draw.ellipse((10, 10, width + 10, height + 10), fill=f"#{color}E8")
    draw.line((18, height // 2 + 10, width + 2, height // 2 + 10), fill=f"#{GREEN_DARK}90", width=max(2, round(3 * scale)))
    leaf = leaf.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    canvas.alpha_composite(leaf, (center[0] - leaf.width // 2, center[1] - leaf.height // 2))


def create_motif(path: Path) -> Image.Image:
    image = Image.new("RGBA", (1600, 430), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    for x in range(40, 650, 62):
        for y in range(48, 380, 62):
            draw.ellipse((x - 2, y - 2, x + 2, y + 2), fill=f"#{GREEN}35")

    circuit = [
        [(70, 210), (260, 210), (260, 140), (470, 140), (470, 105), (640, 105)],
        [(120, 310), (320, 310), (320, 250), (560, 250), (560, 210), (710, 210)],
        [(210, 80), (330, 80), (330, 40), (520, 40)],
    ]
    for points in circuit:
        draw.line(points, fill=f"#{AMBER}B8", width=5, joint="curve")
        for x, y in (points[0], points[-1]):
            draw.ellipse((x - 10, y - 10, x + 10, y + 10), fill=f"#{PAPER}", outline=f"#{AMBER}", width=5)

    branch_points = bezier([(560, 360), (840, 318), (1080, 225), (1510, 72)], 110)
    draw.line(branch_points, fill=f"#{GREEN_DARK}", width=16, joint="curve")
    draw.line(bezier([(820, 300), (920, 180), (1040, 115), (1190, 55)], 50), fill=f"#{GREEN}", width=9)
    draw.line(bezier([(1050, 235), (1140, 320), (1290, 340), (1450, 310)], 50), fill=f"#{GREEN}", width=9)

    leaves = [
        (720, 315, -22, 0.90, GREEN), (820, 270, 25, 0.95, LIME),
        (900, 215, -32, 1.00, GREEN), (985, 190, 28, 0.95, LIME),
        (1080, 145, -28, 1.05, GREEN), (1180, 100, 26, 1.00, LIME),
        (1270, 125, -34, 1.05, GREEN), (1380, 78, 27, 0.95, LIME),
        (930, 145, 52, 0.82, GREEN), (1050, 95, 48, 0.82, LIME),
        (1160, 285, -45, 0.88, GREEN), (1275, 320, 38, 0.92, LIME),
        (1390, 300, -28, 0.90, GREEN),
    ]
    for x, y, angle, scale, color in leaves:
        add_leaf(image, (x, y), angle, scale, color)

    for x, y, radius in [(1018, 244, 21), (1135, 198, 24), (1305, 218, 20), (1440, 155, 23)]:
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=f"#{AMBER}", outline=f"#{GREEN_DARK}", width=4)

    image.save(path)
    return image


def draw_centered(draw: ImageDraw.ImageDraw, text: str, y: int, used_font: ImageFont.ImageFont, fill: str, canvas_width: int) -> None:
    box = draw.textbbox((0, 0), text, font=used_font)
    draw.text(((canvas_width - (box[2] - box[0])) / 2, y), text, font=used_font, fill=fill)


def create_preview(path: Path, motif: Image.Image) -> None:
    image = Image.new("RGB", (1240, 1754), f"#{PAPER}")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 36, 1754), fill=f"#{GREEN}")
    draw.rectangle((36, 0, 1240, 250), fill=f"#{GREEN_DARK}")
    draw.rectangle((92, 72, 212, 192), fill=f"#{LIME}")
    draw.text((122, 91), "榄", font=font(60, True), fill=f"#{GREEN_DARK}")
    draw.text((250, 82), "OLIVE ORCHARD INTELLIGENCE", font=font(26, True), fill=f"#{WHITE}")
    draw.text((250, 132), "AGENT-BASED AGRICULTURAL PRACTICE", font=font(17), fill="#B9C9BF")

    draw.text((100, 360), "榄园知行", font=font(82, True), fill=f"#{GREEN_DARK}")
    draw.rectangle((102, 470, 288, 478), fill=f"#{AMBER}")
    draw.text((100, 520), "智能体开发大赛项目报告", font=font(42, True), fill=f"#{INK}")
    draw.text((102, 590), "面向校园橄榄实训果园的智能管理与农事决策系统", font=font(24), fill=f"#{MUTED}")
    draw.text((102, 635), "RAG 知识检索 · Agent 工具调用 · 农事实训闭环", font=font(18), fill=f"#{GREEN}")

    resized = motif.resize((1080, 290), Image.Resampling.LANCZOS)
    image.paste(resized, (95, 720), resized)

    draw.text((100, 1090), "项目基本信息", font=font(22, True), fill=f"#{GREEN_DARK}")
    draw.rectangle((100, 1135, 1140, 1485), fill=f"#{WHITE}", outline=f"#{LINE}", width=2)
    rows = [
        ("学校名称", "____________________________"),
        ("参赛团队", "____________________________"),
        ("指导教师", "____________________________"),
        ("完成日期", "2026 年 ____ 月"),
    ]
    row_h = 87
    for index, (label, value) in enumerate(rows):
        top = 1136 + index * row_h
        if index:
            draw.line((100, top, 1140, top), fill=f"#{LINE}", width=2)
        draw.rectangle((101, top, 330, top + row_h - 1), fill="#E7EFE9")
        draw.text((150, top + 24), label, font=font(19, True), fill=f"#{GREEN_DARK}")
        draw.text((390, top + 24), value, font=font(19), fill=f"#{MUTED}")

    draw.rectangle((36, 1668, 1240, 1754), fill=f"#{GREEN_DARK}")
    draw.text((100, 1697), "榄园知行项目组", font=font(18, True), fill=f"#{WHITE}")
    right = "2026"
    right_box = draw.textbbox((0, 0), right, font=font(18, True))
    draw.text((1138 - (right_box[2] - right_box[0]), 1697), right, font=font(18, True), fill=f"#{LIME}")
    image.save(path, quality=95)


def x(value: object) -> str:
    return escape(str(value), {'"': '&quot;'})


def run(text: str, size: int = 22, color: str = INK, bold: bool = False, font_name: str = "Microsoft YaHei") -> str:
    props = [
        "<w:rPr>",
        f'<w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:eastAsia="{x(font_name)}"/>',
        f'<w:color w:val="{color}"/>',
        f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>',
    ]
    if bold:
        props.append("<w:b/><w:bCs/>")
    props.append("</w:rPr>")
    pieces: list[str] = []
    for index, line in enumerate(text.split("\n")):
        if index:
            pieces.append("<w:br/>")
        pieces.append(f'<w:t xml:space="preserve">{x(line)}</w:t>')
    return "<w:r>" + "".join(props) + "".join(pieces) + "</w:r>"


def paragraph(text: str = "", *, size: int = 22, color: str = INK, bold: bool = False,
              align: str = "left", before: int = 0, after: int = 120,
              line: int = 300, border_bottom: str | None = None) -> str:
    props = [
        "<w:pPr>",
        f'<w:jc w:val="{align}"/>',
        f'<w:spacing w:before="{before}" w:after="{after}" w:line="{line}" w:lineRule="auto"/>',
    ]
    if border_bottom:
        props.append(f'<w:pBdr><w:bottom w:val="single" w:sz="18" w:space="8" w:color="{border_bottom}"/></w:pBdr>')
    props.append("</w:pPr>")
    return "<w:p>" + "".join(props) + (run(text, size=size, color=color, bold=bold) if text else "") + "</w:p>"


def cell(content: str, width: int, fill: str, *, margins: int = 150, valign: str = "center") -> str:
    return (
        '<w:tc><w:tcPr>'
        f'<w:tcW w:w="{width}" w:type="dxa"/><w:shd w:val="clear" w:color="auto" w:fill="{fill}"/>'
        f'<w:vAlign w:val="{valign}"/><w:tcMar>'
        f'<w:top w:w="{margins}" w:type="dxa"/><w:left w:w="{margins}" w:type="dxa"/>'
        f'<w:bottom w:w="{margins}" w:type="dxa"/><w:right w:w="{margins}" w:type="dxa"/>'
        '</w:tcMar></w:tcPr>' + content + '</w:tc>'
    )


def top_band() -> str:
    logo = paragraph("榄", size=68, color=GREEN_DARK, bold=True, align="center", after=0)
    brand = (
        paragraph("OLIVE ORCHARD INTELLIGENCE", size=24, color=WHITE, bold=True, after=30)
        + paragraph("AGENT-BASED AGRICULTURAL PRACTICE", size=16, color="B9C9BF", after=0)
    )
    return (
        '<w:tbl><w:tblPr><w:tblW w:w="9000" w:type="dxa"/><w:tblLayout w:type="fixed"/>'
        '<w:tblBorders><w:top w:val="nil"/><w:left w:val="nil"/><w:bottom w:val="nil"/>'
        '<w:right w:val="nil"/><w:insideH w:val="nil"/><w:insideV w:val="nil"/></w:tblBorders></w:tblPr>'
        '<w:tblGrid><w:gridCol w:w="1450"/><w:gridCol w:w="7550"/></w:tblGrid>'
        '<w:tr><w:trPr><w:trHeight w:val="1450" w:hRule="exact"/></w:trPr>'
        + cell(logo, 1450, LIME, margins=90)
        + cell(brand, 7550, GREEN_DARK, margins=300)
        + '</w:tr></w:tbl>'
    )


def metadata_table() -> str:
    rows = [
        ("学校名称", "____________________________"),
        ("参赛团队", "____________________________"),
        ("指导教师", "____________________________"),
        ("完成日期", "2026 年 ____ 月"),
    ]
    result = [
        '<w:tbl><w:tblPr><w:tblW w:w="9000" w:type="dxa"/><w:tblLayout w:type="fixed"/>'
        '<w:tblBorders><w:top w:val="single" w:sz="6" w:color="D8E0D9"/>'
        '<w:left w:val="single" w:sz="6" w:color="D8E0D9"/>'
        '<w:bottom w:val="single" w:sz="6" w:color="D8E0D9"/>'
        '<w:right w:val="single" w:sz="6" w:color="D8E0D9"/>'
        '<w:insideH w:val="single" w:sz="4" w:color="D8E0D9"/>'
        '<w:insideV w:val="nil"/></w:tblBorders></w:tblPr>'
        '<w:tblGrid><w:gridCol w:w="2050"/><w:gridCol w:w="6950"/></w:tblGrid>'
    ]
    for label, value in rows:
        label_p = paragraph(label, size=20, color=GREEN_DARK, bold=True, align="center", after=0)
        value_p = paragraph(value, size=20, color=MUTED, after=0)
        result.append('<w:tr><w:trPr><w:trHeight w:val="620" w:hRule="atLeast"/></w:trPr>')
        result.append(cell(label_p, 2050, "E7EFE9", margins=130))
        result.append(cell(value_p, 6950, WHITE, margins=180))
        result.append('</w:tr>')
    result.append('</w:tbl>')
    return "".join(result)


def image_paragraph(width_px: int, height_px: int) -> str:
    width_emu = 5_715_000
    height_emu = round(width_emu * height_px / width_px)
    return f'''<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="160" w:after="160"/></w:pPr><w:r><w:drawing>
<wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" distT="0" distB="0" distL="0" distR="0">
  <wp:extent cx="{width_emu}" cy="{height_emu}"/><wp:docPr id="1" name="Olive agent motif"/>
  <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
    <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
      <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
        <pic:nvPicPr><pic:cNvPr id="0" name="olive-agent-motif.png"/><pic:cNvPicPr/></pic:nvPicPr>
        <pic:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>
        <pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{width_emu}" cy="{height_emu}"/></a:xfrm>
          <a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:noFill/><a:ln><a:noFill/></a:ln>
        </pic:spPr>
      </pic:pic>
    </a:graphicData>
  </a:graphic>
</wp:inline></w:drawing></w:r></w:p>'''


def document_xml(motif: Image.Image) -> str:
    body = [
        top_band(),
        paragraph("榄园知行", size=94, color=GREEN_DARK, bold=True, before=460, after=90),
        paragraph("智能体开发大赛项目报告", size=42, color=INK, bold=True, after=120, border_bottom=AMBER),
        paragraph("面向校园橄榄实训果园的智能管理与农事决策系统", size=27, color=MUTED, after=80),
        paragraph("RAG 知识检索  ·  Agent 工具调用  ·  农事实训闭环", size=18, color=GREEN, bold=True, after=30),
        image_paragraph(motif.width, motif.height),
        paragraph("项目基本信息", size=22, color=GREEN_DARK, bold=True, before=80, after=100),
        metadata_table(),
        paragraph("榄园知行项目组  ·  2026", size=17, color=MUTED, align="right", before=260, after=0),
    ]
    sect = (
        '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>'
        '<w:pgMar w:top="760" w:right="1100" w:bottom="720" w:left="1100" w:header="360" w:footer="360" w:gutter="0"/>'
        '<w:cols w:space="720"/><w:docGrid w:linePitch="312"/></w:sectPr>'
    )
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="{W}" xmlns:r="{R}" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:background w:color="{PAPER}"/>
  <w:body>{''.join(body)}{sect}</w:body>
</w:document>'''


def styles_xml() -> str:
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="{W}">
  <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:eastAsia="Microsoft YaHei"/><w:color w:val="{INK}"/><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:line="300" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>
</w:styles>'''


def content_types() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
<Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
<Override PartName="/word/fontTable.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>'''


def build_docx(path: Path, motif: Image.Image) -> None:
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    files: dict[str, str | bytes] = {
        "[Content_Types].xml": content_types(),
        "_rels/.rels": '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>''',
        "word/document.xml": document_xml(motif),
        "word/_rels/document.xml.rels": '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/olive-agent-motif.png"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
<Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
</Relationships>''',
        "word/styles.xml": styles_xml(),
        "word/settings.xml": f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:settings xmlns:w="{W}"><w:displayBackgroundShape/><w:zoom w:percent="85"/><w:doNotHyphenateCaps/></w:settings>''',
        "word/fontTable.xml": f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:fonts xmlns:w="{W}"><w:font w:name="Microsoft YaHei"/><w:font w:name="Arial"/></w:fonts>''',
        "word/media/olive-agent-motif.png": MOTIF_OUT.read_bytes(),
        "docProps/core.xml": f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>榄园知行——项目报告封面</dc:title><dc:creator>榄园知行项目组</dc:creator><dc:subject>智能体开发大赛项目报告</dc:subject><dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified></cp:coreProperties>''',
        "docProps/app.xml": '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft Office Word</Application><AppVersion>16.0000</AppVersion></Properties>''',
    }
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as package:
        for name, data in files.items():
            package.writestr(name, data.encode("utf-8") if isinstance(data, str) else data)


def main() -> None:
    motif = create_motif(MOTIF_OUT)
    create_preview(PREVIEW_OUT, motif)
    build_docx(DOCX_OUT, motif)
    print(DOCX_OUT)
    print(PREVIEW_OUT)


if __name__ == "__main__":
    main()
