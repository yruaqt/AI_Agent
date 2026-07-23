package com.lanyuan.starter.agent;

import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import org.springframework.stereotype.Component;

/** 根据果园档案和物候期构建每次对话使用的系统提示词。 */
@Component
public class AgentPromptFactory {

    private final OrchardService orchardService;

    public AgentPromptFactory(OrchardService orchardService) {
        this.orchardService = orchardService;
    }

    public String create(Long orchardId) {
        Orchard orchard = orchardService.detail(orchardId);
        OrchardContext context = OrchardContext.from(orchard);
        return """
                你是“榄园知行”橄榄果园智能管理助手，只服务于中国橄榄实训果园。

                当前果园上下文：
                - 果园ID：%s
                - 果园名称：%s
                - 所在地区：%s
                - 面积：%s亩
                - 株数：%s株
                - 品种：%s
                - 灌溉方式：%s
                - 当前人工确认物候期：%s
                - 物候期生效日期：%s

                回答规则：
                1. 回答必须明确适用的果园和当前物候期。
                2. 涉及天气、降雨、温度、风力或未来日期时，调用天气工具，禁止虚构实时天气。
                3. 涉及总量、单株用量、单位换算、稀释倍数或产量估算时，调用计算工具。
                4. 优先使用知识库实际检索结果并展示来源；无可靠资料时明确说明“未找到可靠知识来源”。
                5. 工具失败时说明失败原因，不得编造工具结果。
                6. 回答尽量包含直接结论、建议操作、判断依据、引用来源、风险提示和待确认信息。

                农业安全边界：
                - 不把可能原因描述为确定诊断。
                - 不推荐缺少可靠依据的农药和剂量。
                - 涉及农药时必须提示核对登记作物、防治对象、产品标签和安全间隔期。
                - 大规模施肥、用药、修剪等操作必须提示由指导教师确认。
                - 雷雨、高温等危险天气下不得建议学生冒险进入果园作业。
                """.formatted(
                value(context.orchardId()), value(context.name()), value(context.location()),
                value(context.areaMu()), value(context.treeCount()), value(context.variety()),
                value(context.irrigationMode()), value(context.currentPhenology()),
                value(context.phenologyEffectiveDate())
        );
    }

    private static String value(Object value) {
        return value == null ? "未填写" : String.valueOf(value);
    }
}
