package com.lanyuan.starter.config;

import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.knowledge.DocumentStatus;
import com.lanyuan.starter.knowledge.KnowledgeChunk;
import com.lanyuan.starter.knowledge.KnowledgeChunkRepository;
import com.lanyuan.starter.knowledge.KnowledgeDocument;
import com.lanyuan.starter.knowledge.KnowledgeDocumentRepository;
import com.lanyuan.starter.knowledge.KnowledgeFileType;
import com.lanyuan.starter.knowledge.KnowledgeStorageProperties;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardRepository;
import com.lanyuan.starter.orchard.PhenologyRecord;
import com.lanyuan.starter.orchard.PhenologyRepository;
import com.lanyuan.starter.orchard.PhenologyStage;
import com.lanyuan.starter.rag.EmbeddingVector;
import com.lanyuan.starter.rag.PgVectorStore;
import com.lanyuan.starter.rag.RagEmbeddingService;
import com.lanyuan.starter.rag.VectorCodec;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.task.FarmingTask;
import com.lanyuan.starter.task.FarmingTaskRepository;
import com.lanyuan.starter.task.TaskPriority;
import com.lanyuan.starter.task.TaskStatus;
import com.lanyuan.starter.training.TrainingRecord;
import com.lanyuan.starter.training.TrainingRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 显式开启 app.demo-data.enabled 后，幂等补充可直接用于页面展示和 RAG 检索的演示数据。
 * 已存在的账号不会被覆盖或重置密码，重复启动也不会重复插入同一批数据。
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class SeedData implements CommandLineRunner {

    private final OrchardRepository orchardRepository;
    private final PhenologyRepository phenologyRepository;
    private final UserRepository userRepository;
    private final FarmingTaskRepository taskRepository;
    private final TrainingRecordRepository trainingRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final RagEmbeddingService embeddingService;
    private final PgVectorStore pgVectorStore;
    private final KnowledgeStorageProperties storageProperties;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public SeedData(OrchardRepository orchardRepository,
                    PhenologyRepository phenologyRepository,
                    UserRepository userRepository,
                    FarmingTaskRepository taskRepository,
                    TrainingRecordRepository trainingRepository,
                    KnowledgeDocumentRepository documentRepository,
                    KnowledgeChunkRepository chunkRepository,
                    RagEmbeddingService embeddingService,
                    PgVectorStore pgVectorStore,
                    KnowledgeStorageProperties storageProperties,
                    PasswordEncoder passwordEncoder,
                    @Value("${app.demo-data.password:123456}") String demoPassword) {
        this.orchardRepository = orchardRepository;
        this.phenologyRepository = phenologyRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.trainingRepository = trainingRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.pgVectorStore = pgVectorStore;
        this.storageProperties = storageProperties;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) throws IOException {
        if (demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalStateException("开启演示数据时 DEMO_PASSWORD 不能为空");
        }

        AppUser admin = createUserIfMissing("admin", "系统管理员", UserRole.ADMIN);
        AppUser student = createUserIfMissing("student", "演示学生", UserRole.STUDENT);
        AppUser student02 = createUserIfMissing("student02", "农技二组学生", UserRole.STUDENT);
        AppUser student03 = createUserIfMissing("student03", "植保三组学生", UserRole.STUDENT);

        Orchard east = createOrchardIfMissing(new OrchardSeed(
                "学校东区橄榄实训果园", "5.00", 300, 6, "本校主栽青橄榄品种",
                "露地栽培", "滴灌", LocalDate.of(2020, 3, 1), "广东省", "广州市", "海珠区",
                113.35, 23.12, "实训指导教师", PhenologyStage.FRUIT_EXPANSION,
                LocalDate.of(2026, 6, 20), "用于水肥、修剪和生长观测的综合实训果园"));
        Orchard north = createOrchardIfMissing(new OrchardSeed(
                "学校北区品种教学园", "3.60", 216, 4, "潮阳三棱橄榄",
                "起垄栽培", "微喷灌", LocalDate.of(2022, 2, 18), "广东省", "广州市", "白云区",
                113.27, 23.21, "林老师", PhenologyStage.MATURITY,
                LocalDate.of(2026, 7, 15), "用于品种比较、成熟度判断和采收实训"));
        Orchard disease = createOrchardIfMissing(new OrchardSeed(
                "病虫害绿色防控示范园", "8.20", 492, 8, "乌橄榄",
                "生态草生栽培", "水肥一体化", LocalDate.of(2018, 3, 12), "广东省", "佛山市", "南海区",
                113.13, 23.02, "陈老师", PhenologyStage.FRUIT_SET,
                LocalDate.of(2026, 6, 28), "重点展示病虫监测、生物防治和安全用药"));

        seedPhenology(east, List.of(
                new PhenologySeed(PhenologyStage.FLOWERING, LocalDate.of(2026, 4, 8), "初花率达到约 20%"),
                new PhenologySeed(PhenologyStage.FRUIT_SET, LocalDate.of(2026, 5, 18), "谢花后坐果稳定"),
                new PhenologySeed(PhenologyStage.FRUIT_EXPANSION, LocalDate.of(2026, 6, 20), "教师现场确认进入幼果膨大期")));
        seedPhenology(north, List.of(
                new PhenologySeed(PhenologyStage.FRUIT_SET, LocalDate.of(2026, 5, 12), "完成第一次坐果调查"),
                new PhenologySeed(PhenologyStage.FRUIT_EXPANSION, LocalDate.of(2026, 6, 16), "果径开始快速增长"),
                new PhenologySeed(PhenologyStage.MATURITY, LocalDate.of(2026, 7, 15), "部分果实达到教学采收成熟度")));
        seedPhenology(disease, List.of(
                new PhenologySeed(PhenologyStage.SHOOT_GROWTH, LocalDate.of(2026, 3, 16), "春梢抽生整齐"),
                new PhenologySeed(PhenologyStage.FLOWERING, LocalDate.of(2026, 4, 20), "进入盛花观察期"),
                new PhenologySeed(PhenologyStage.FRUIT_SET, LocalDate.of(2026, 6, 28), "坐果后开展病虫害基数调查")));

        seedKnowledgeBase();
        seedTasks(admin, student, student02, student03, east, north, disease);
        seedTrainingRecords(student, student02, student03, east, north, disease);
    }

    private AppUser createUserIfMissing(String username, String displayName, UserRole role) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
                new AppUser(username, passwordEncoder.encode(demoPassword), displayName, role)));
    }

    private Orchard createOrchardIfMissing(OrchardSeed seed) {
        return orchardRepository.findByName(seed.name()).orElseGet(() -> {
            Orchard orchard = new Orchard();
            orchard.setName(seed.name());
            orchard.setAreaMu(new BigDecimal(seed.areaMu()));
            orchard.setTreeCount(seed.treeCount());
            orchard.setTreeAgeYears(seed.treeAgeYears());
            orchard.setVariety(seed.variety());
            orchard.setPlantingMode(seed.plantingMode());
            orchard.setIrrigationMode(seed.irrigationMode());
            orchard.setPlantingDate(seed.plantingDate());
            orchard.setProvince(seed.province());
            orchard.setCity(seed.city());
            orchard.setDistrict(seed.district());
            orchard.setLongitude(seed.longitude());
            orchard.setLatitude(seed.latitude());
            orchard.setManagerName(seed.managerName());
            orchard.setCurrentPhenology(seed.currentPhenology());
            orchard.setPhenologyEffectiveDate(seed.phenologyEffectiveDate());
            orchard.setRemark(seed.remark());
            return orchardRepository.save(orchard);
        });
    }

    private void seedPhenology(Orchard orchard, List<PhenologySeed> seeds) {
        for (PhenologySeed seed : seeds) {
            if (phenologyRepository.existsByOrchardIdAndPhenologyAndEffectiveDate(
                    orchard.getId(), seed.stage(), seed.effectiveDate())) continue;
            PhenologyRecord record = new PhenologyRecord();
            record.setOrchardId(orchard.getId());
            record.setPhenology(seed.stage());
            record.setEffectiveDate(seed.effectiveDate());
            record.setRemark(seed.remark());
            phenologyRepository.save(record);
        }
    }

    private void seedTasks(AppUser admin, AppUser student, AppUser student02, AppUser student03,
                           Orchard east, Orchard north, Orchard disease) {
        createTaskIfMissing(2026070101L, east, LocalDate.of(2026, 7, 28), "水肥管理",
                "幼果膨大期滴灌与追肥", "检查土壤墒情后分区滴灌，并少量多次补充高钾复合肥，记录灌溉时长和用肥量。",
                TaskPriority.HIGH, "07:00—09:00", TaskStatus.TODO,
                "幼果膨大期需水需钾量上升，应避免一次性大水大肥。", "施肥时佩戴手套，肥料不得直接接触树干。",
                null, student.getId(), admin.getId());
        createTaskIfMissing(2026070101L, east, LocalDate.of(2026, 7, 27), "生长调查",
                "抽样测量果径与新梢长度", "按东南西北四个方位选取 20 株样树，测量果径、新梢长度并录入实训记录。",
                TaskPriority.MEDIUM, "08:00—10:00", TaskStatus.DOING,
                "连续监测可判断膨大速度和肥水措施效果。", "使用游标卡尺时注意夹持力度，避免损伤果实。",
                "二组已完成东侧样树测量", student02.getId(), admin.getId());
        createTaskIfMissing(2026070102L, east, LocalDate.of(2026, 7, 23), "整形修剪",
                "清理内膛徒长枝和病弱枝", "优先剪除交叉枝、病弱枝和遮光严重的徒长枝，保持树冠通风透光。",
                TaskPriority.MEDIUM, "阴天或上午", TaskStatus.DONE,
                "改善通风透光可降低病害发生并减少无效养分消耗。", "修枝剪使用前后消毒，高处作业必须两人配合。",
                "已完成 60 株，工具已消毒归还", student.getId(), admin.getId());
        createTaskIfMissing(2026070201L, north, LocalDate.of(2026, 7, 29), "采收准备",
                "开展成熟度抽检并划分采收批次", "随机抽取不同冠层果实，记录果色、果径和硬度，标记先采与后采区域。",
                TaskPriority.HIGH, "06:30—09:30", TaskStatus.CONFIRMED,
                "分批采收能够兼顾商品成熟度与运输耐受性。", "抽样梯必须放稳，禁止攀爬湿滑树体。",
                null, student03.getId(), admin.getId());
        createTaskIfMissing(2026070202L, north, LocalDate.of(2026, 7, 25), "设施维护",
                "检查微喷系统压力与堵塞点", "逐区开启微喷，记录压力异常、喷头堵塞和管线渗漏位置并完成清洗。",
                TaskPriority.LOW, "16:00—18:00", TaskStatus.DONE,
                "均匀供水是成熟期稳定果实品质的基础。", "检修前关闭分区阀门，禁止带压拆卸喷头。",
                "更换喷头 3 个，修复支管渗漏 1 处", student02.getId(), admin.getId());
        createTaskIfMissing(2026070301L, disease, LocalDate.of(2026, 7, 28), "病虫监测",
                "调查炭疽病和橄榄星室木虱", "采用五点取样法检查叶片、嫩梢和幼果，分别统计病叶率、虫梢率并拍照留档。",
                TaskPriority.HIGH, "07:30—10:00", TaskStatus.TODO,
                "高温高湿天气需加密炭疽病和刺吸式害虫监测。", "进入示范园穿长袖工作服，不直接触碰未知虫体。",
                null, student03.getId(), admin.getId());
        createTaskIfMissing(2026070301L, disease, LocalDate.of(2026, 7, 27), "绿色防控",
                "更换诱虫板并记录虫口数量", "按编号检查黄色诱虫板，统计主要害虫数量，更换粘性下降或污染严重的诱虫板。",
                TaskPriority.MEDIUM, "16:30—18:00", TaskStatus.DOING,
                "诱虫板数据可辅助判断虫口增长趋势和防治窗口。", "废弃诱虫板装袋回收，不得遗留在园内。",
                "已完成 A、B 两区，C 区待继续", student02.getId(), admin.getId());
        createTaskIfMissing(2026070302L, disease, LocalDate.of(2026, 7, 21), "园区卫生",
                "清除落果和病残枝", "收集园内落果、病叶和修剪残枝，分类装袋并转移到指定处理点。",
                TaskPriority.MEDIUM, "全天分组完成", TaskStatus.DONE,
                "及时清园可减少病原菌和害虫的园内循环。", "病残体不得随意堆放或直接带出示范区。",
                "三组联合完成，现场复核合格", student03.getId(), admin.getId());
    }

    private void createTaskIfMissing(long batchId, Orchard orchard, LocalDate date, String type,
                                     String title, String content, TaskPriority priority, String suggestedTime,
                                     TaskStatus status, String basis, String safetyNotice, String statusRemark,
                                     Long assigneeId, Long generatedBy) {
        if (taskRepository.existsByOrchardIdAndTitle(orchard.getId(), title)) return;
        FarmingTask task = new FarmingTask();
        task.setBatchId(batchId);
        task.setOrchardId(orchard.getId());
        task.setTaskDate(date);
        task.setType(type);
        task.setTitle(title);
        task.setContent(content);
        task.setPriority(priority);
        task.setSuggestedTime(suggestedTime);
        task.setStatus(status);
        task.setBasis(basis);
        task.setSafetyNotice(safetyNotice);
        task.setStatusRemark(statusRemark);
        task.setAssigneeId(assigneeId);
        task.setGeneratedBy(generatedBy);
        taskRepository.save(task);
    }

    private void seedTrainingRecords(AppUser student, AppUser student02, AppUser student03,
                                     Orchard east, Orchard north, Orchard disease) {
        createTrainingIfMissing(student, east, LocalDate.of(2026, 7, 18), 30, 2,
                "部分幼果表面出现轻微日灼斑，西侧树冠较明显。",
                "补充树盘覆盖并调整中午时段灌溉安排。", "三天后新发日灼数量明显减少。",
                "观察过程完整，建议补充天气数据。", 88, "APPROVED");
        createTrainingIfMissing(student02, east, LocalDate.of(2026, 7, 23), 60, 4,
                "内膛枝条密集，4 株发现枯弱枝。",
                "完成疏枝并对剪口和工具进行消毒。", "树冠通风透光改善，病弱枝已集中处理。",
                "操作规范，修剪尺度控制合理。", 92, "APPROVED");
        createTrainingIfMissing(student03, north, LocalDate.of(2026, 7, 20), 24, 0,
                "抽样果实颜色由深绿转黄绿，平均果径达到教学采收标准。",
                "按地块标记成熟度等级并提交分批采收建议。", "形成两批采收清单。",
                "抽样点分布合理。", 90, "APPROVED");
        createTrainingIfMissing(student02, north, LocalDate.of(2026, 7, 25), 18, 3,
                "发现 3 个微喷头出水不均，其中 1 个完全堵塞。",
                "拆洗喷头并冲洗末端管路。", "复测后各喷头出水恢复正常。",
                null, null, "PENDING");
        createTrainingIfMissing(student03, disease, LocalDate.of(2026, 7, 22), 50, 7,
                "低洼区叶片炭疽病斑较多，雨后湿度持续偏高。",
                "清除重病叶，记录病株位置并建议加强排水。", "病残体已集中装袋，等待教师复核。",
                "病害识别准确，后续需连续监测。", 86, "APPROVED");
        createTrainingIfMissing(student, disease, LocalDate.of(2026, 7, 26), 40, 5,
                "黄色诱虫板上星室木虱数量较上周增加。",
                "更换诱虫板并记录不同区域虫量。", "已完成 A、B 区统计，C 区尚未完成。",
                "数据未完整，补充后再审核。", 72, "REJECTED");
    }

    private void createTrainingIfMissing(AppUser student, Orchard orchard, LocalDate date,
                                         Integer inspected, Integer abnormal, String phenomenon,
                                         String measure, String result, String comment,
                                         Integer score, String reviewStatus) {
        if (trainingRepository.existsByStudentIdAndOrchardIdAndRecordDateAndPhenomenon(
                student.getId(), orchard.getId(), date, phenomenon)) return;
        TrainingRecord record = new TrainingRecord();
        record.setStudentId(student.getId());
        record.setOrchardId(orchard.getId());
        record.setRecordDate(date);
        record.setInspectedTreeCount(inspected);
        record.setAbnormalTreeCount(abnormal);
        record.setPhenomenon(phenomenon);
        record.setMeasure(measure);
        record.setResult(result);
        record.setComment(comment);
        record.setScore(score);
        record.setReviewStatus(reviewStatus);
        trainingRepository.save(record);
    }

    private void seedKnowledgeBase() throws IOException {
        List<KnowledgeSeed> documents = List.of(
                new KnowledgeSeed("幼果膨大期水肥管理技术要点", "广东省现代农业实训中心",
                        LocalDate.of(2026, 5, 20), "广东省", "FRUIT_EXPANSION", "农技资料",
                        "seed-fruit-expansion.md", List.of(
                        "幼果膨大期是橄榄果实体积快速增加的阶段。应先检查土壤墒情，再采用少量多次的滴灌方式补水，避免长期积水造成根系缺氧。高温晴天宜在清晨或傍晚灌溉，暴雨后应暂停灌水并及时排除低洼处积水。",
                        "施肥以平衡供应为原则，可适当提高钾肥比例并配合腐熟有机肥。严禁肥料直接堆放在树干基部，沟施或滴灌施肥后应观察叶色、新梢和果径变化，发现叶尖焦枯时及时排查肥害。",
                        "实训记录至少包括日期、天气、灌溉时长、肥料名称和用量、样树编号、果径与异常情况。同一批样树应固定测量位置，便于比较不同日期的膨大速度。")),
                new KnowledgeSeed("橄榄园暴雨与台风前后管理", "广东省岭南果树技术推广站",
                        LocalDate.of(2026, 6, 10), "广东省", "FRUIT_EXPANSION", "技术规程",
                        "seed-storm-management.md", List.of(
                        "台风和暴雨到来前，应清理主排水沟和支沟，检查棚架、供电和水肥管线，幼树设支柱并使用软质绑带固定。树冠过密时可适度疏除枯枝和交叉枝，但不宜在灾前进行重剪。",
                        "灾后首先确认人员和用电安全，再检查倒伏、折枝、积水和根系裸露情况。轻度倾斜植株可扶正培土并设支撑，折裂枝从健康部位回缩，剪锯口与工具应及时消毒。",
                        "连续降雨后炭疽病等病害风险升高，应加强叶片、嫩梢和果实巡查，及时清除病残体并改善通风。需要用药时严格遵守登记作物、剂量、安全间隔期和个人防护要求。")),
                new KnowledgeSeed("青橄榄常见病虫害识别与绿色防治", "校企合作植保教研组",
                        LocalDate.of(2026, 4, 28), "广东省", "FRUIT_SET", "校本资料",
                        "seed-pest-control.md", List.of(
                        "炭疽病常在叶片和果实形成褐色至黑褐色病斑，高温高湿、树冠郁闭和病残体积累会加重发生。调查时应分别记录调查叶果总数和发病数量，不能只记录发现病斑的树数。",
                        "橄榄星室木虱等刺吸式害虫主要危害嫩梢。可通过检查卷曲嫩叶、虫体和分泌物识别，并结合黄色诱虫板连续监测。不同日期的诱虫板应保持位置、悬挂高度和统计口径一致。",
                        "绿色防控优先采用清园、合理修剪、保护天敌和诱虫监测。达到防治指标后再选择合规药剂，交替使用不同作用机制，严禁随意提高浓度或缩短用药间隔。")),
                new KnowledgeSeed("橄榄施肥与土壤管理规程", "学校橄榄栽培课程组",
                        LocalDate.of(2026, 3, 15), "广东省", "SHOOT_GROWTH", "技术规程",
                        "seed-soil-fertilization.md", List.of(
                        "施肥方案应综合树龄、树势、目标产量、物候期和土壤检测结果制定。幼树重点促进树冠形成，结果树应避免偏施氮肥导致徒长和落果。每次施肥后记录肥料批次、用量、施用区域和操作人员。",
                        "树盘可采用有机覆盖或生态草生方式减少水分蒸发和地表冲刷。覆盖物与树干保持距离，雨季及时疏通排水，黏重土壤避免在过湿条件下反复踩踏和机械作业。",
                        "出现黄化、落叶或生长停滞时，不应立即盲目加肥。应先检查根区积水、土壤酸碱度、肥害、病虫害和微量元素缺乏，并通过对照样树验证处理效果。")),
                new KnowledgeSeed("橄榄采收及采后保鲜操作规范", "岭南果品产后处理实训基地",
                        LocalDate.of(2026, 7, 5), "广东省", "MATURITY", "农技资料",
                        "seed-harvest-storage.md", List.of(
                        "采收前根据用途评价果色、果径、硬度和风味，分区域抽样并划分采收批次。鲜食果宜在露水干后、气温较低时人工采摘，避免拉扯果枝和将果实直接抛入容器。",
                        "采收容器应清洁、内壁光滑，装果不宜过满。果实到达处理点后尽快剔除机械伤、病斑和过熟果，按大小和成熟度分级，减少反复翻倒造成的碰压伤。",
                        "预冷和低温贮运有助于降低呼吸消耗，但温湿度应根据品种和用途设置。采收批次、地块、时间、数量、分级结果和异常情况必须可追溯，发现腐烂批次应隔离检查。"))
        );

        Files.createDirectories(storageProperties.getDirectory());
        for (KnowledgeSeed seed : documents) seedKnowledgeDocument(seed);
    }

    private void seedKnowledgeDocument(KnowledgeSeed seed) throws IOException {
        Path path = storageProperties.getDirectory().resolve(seed.filename()).normalize();
        if (!path.startsWith(storageProperties.getDirectory())) {
            throw new IllegalStateException("知识库演示文件路径非法");
        }
        if (Files.notExists(path)) {
            Files.writeString(path, markdown(seed), StandardCharsets.UTF_8);
        }

        KnowledgeDocument document = documentRepository.findByTitleAndDeletedFalse(seed.title()).orElse(null);
        if (document == null) {
            document = new KnowledgeDocument();
            document.setTitle(seed.title());
            document.setSourceOrganization(seed.sourceOrganization());
            document.setPublishDate(seed.publishDate());
            document.setRegion(seed.region());
            document.setPhenology(seed.phenology());
            document.setDocumentType(seed.documentType());
            document.setOriginalName(seed.title() + ".md");
            document.setFileType(KnowledgeFileType.MARKDOWN);
            document.setStoragePath(path.toString());
            document.setStatus(DocumentStatus.PENDING);
            document = documentRepository.saveAndFlush(document);
        }

        List<KnowledgeChunk> existingChunks = chunkRepository.findByDocumentIdOrderByChunkIndex(document.getId());
        if (document.getStatus() == DocumentStatus.SUCCESS && existingChunks.size() == seed.chunks().size()
                && existingChunks.stream().allMatch(chunk -> chunk.getEmbeddingData() != null)) {
            return;
        }

        chunkRepository.deleteByDocumentId(document.getId());
        OffsetDateTime indexedAt = OffsetDateTime.now();
        for (int index = 0; index < seed.chunks().size(); index++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(index);
            chunk.setContent(seed.chunks().get(index));
            chunk.setPageNumber(index + 1);
            chunk.setRegion(seed.region());
            chunk.setPhenology(seed.phenology());
            chunk.setDocumentType(seed.documentType());
            EmbeddingVector vector = embeddingService.embedForProvider(chunk.getContent(), RagEmbeddingService.LOCAL_HASH);
            chunk.setEmbeddingProvider(vector.provider());
            chunk.setEmbeddingDimension(vector.dimension());
            chunk.setEmbeddingData(VectorCodec.encode(vector.values()));
            chunk.setIndexedAt(indexedAt);
            chunkRepository.saveAndFlush(chunk);
            pgVectorStore.sync(chunk);
        }
        document.setStoragePath(path.toString());
        document.setChunkCount(seed.chunks().size());
        document.setStatus(DocumentStatus.SUCCESS);
        document.setFailureReason(null);
        documentRepository.save(document);
    }

    private static String markdown(KnowledgeSeed seed) {
        StringBuilder content = new StringBuilder("# ").append(seed.title()).append("\n\n")
                .append("来源：").append(seed.sourceOrganization()).append("\n\n");
        for (int index = 0; index < seed.chunks().size(); index++) {
            content.append("## 要点 ").append(index + 1).append("\n\n")
                    .append(seed.chunks().get(index)).append("\n\n");
        }
        return content.toString();
    }

    private record OrchardSeed(String name, String areaMu, int treeCount, int treeAgeYears,
                               String variety, String plantingMode, String irrigationMode,
                               LocalDate plantingDate, String province, String city, String district,
                               double longitude, double latitude, String managerName,
                               PhenologyStage currentPhenology, LocalDate phenologyEffectiveDate,
                               String remark) {}

    private record PhenologySeed(PhenologyStage stage, LocalDate effectiveDate, String remark) {}

    private record KnowledgeSeed(String title, String sourceOrganization, LocalDate publishDate,
                                 String region, String phenology, String documentType,
                                 String filename, List<String> chunks) {}
}
