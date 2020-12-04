package net.zlw.cloud.designProject.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import net.tec.cloud.common.bean.UserInfo;
import net.zlw.cloud.VisaChange.mapper.VisaApplyChangeInformationMapper;
import net.zlw.cloud.budgeting.mapper.CostPreparationDao;
import net.zlw.cloud.budgeting.mapper.VeryEstablishmentDao;
import net.zlw.cloud.budgeting.model.CostPreparation;
import net.zlw.cloud.budgeting.model.VeryEstablishment;
import net.zlw.cloud.buildingProject.mapper.BuildingProjectMapper;
import net.zlw.cloud.buildingProject.model.BuildingProject;
import net.zlw.cloud.designProject.mapper.*;
import net.zlw.cloud.designProject.model.*;
import net.zlw.cloud.followAuditing.mapper.TrackAuditInfoDao;
import net.zlw.cloud.followAuditing.model.TrackAuditInfo;
import net.zlw.cloud.index.mapper.MessageNotificationDao;
import net.zlw.cloud.index.model.MessageNotification;
import net.zlw.cloud.progressPayment.mapper.AuditInfoDao;
import net.zlw.cloud.progressPayment.mapper.MemberManageDao;
import net.zlw.cloud.progressPayment.mapper.ProgressPaymentInformationDao;
import net.zlw.cloud.progressPayment.model.AuditInfo;
import net.zlw.cloud.remindSet.mapper.RemindSetMapper;
import net.zlw.cloud.settleAccounts.mapper.LastSettlementReviewDao;
import net.zlw.cloud.settleAccounts.mapper.SettlementAuditInformationDao;
import net.zlw.cloud.snsEmailFile.mapper.FileInfoMapper;
import net.zlw.cloud.snsEmailFile.model.FileInfo;
import net.zlw.cloud.snsEmailFile.model.MkyUser;
import net.zlw.cloud.snsEmailFile.model.vo.MessageVo;
import net.zlw.cloud.snsEmailFile.service.MessageService;
import net.zlw.cloud.warningDetails.model.MemberManage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ProjectService {

    @Resource
    private EmployeeAchievementsInfoMapper employeeAchievementsInfoMapper;

    @Resource
    private ProjectMapper projectMapper;  //基本信息

    @Resource
    private DesignInfoMapper designInfoMapper; //设计信息

    @Resource
    private DesignChangeInfoMapper designChangeInfoMapper;  //设计变更信息

    @Resource
    private BudgetingMapper budgetingMapper; //预算编译信息

    @Resource
    private WujiangMoneyInfoMapper wujiangMoneyInfoMapper; //吴江设计费\

    @Resource
    private AnhuiMoneyinfoMapper anhuiMoneyinfoMapper;

    @Resource
    private AuditInfoDao auditInfoDao;

    @Resource
    private PackageCameMapper packageCameMapper;

    @Resource
    private ProjectExplorationMapper projectExplorationMapper;

    @Resource
    private MemberManageDao memberManageDao;

    @Resource
    private MunicipalNgineerDesignMapper municipalNgineerDesignMapper;

    @Resource
    private CostPreparationDao costPreparationDao;

    @Resource
    private VeryEstablishmentDao veryEstablishmentDao;

    @Resource
    private MessageNotificationDao messageNotificationDao;

    @Resource
    private TrackAuditInfoDao trackAuditInfoDao;

    @Resource
    private SettlementAuditInformationDao settlementAuditInformationDao;

    @Resource
    private LastSettlementReviewDao lastSettlementReviewDao;

    @Resource
    private ProgressPaymentInformationDao progressPaymentInformationDao;

    @Resource
    private VisaApplyChangeInformationMapper visaApplyChangeInformationMapper;

    @Resource
    private BuildingProjectMapper buildingProjectMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private OutSourceMapper outSourceMapper;

    @Value("${audit.wujiang.sheji.designHead}")
    private String wjsjh;
    @Value("${audit.wujiang.sheji.designManager}")
    private String wjsjm;
    @Value("${audit.wujiang.zaojia.costHead}")
    private String wjzjh;
    @Value("${audit.wujiang.zaojia.costManager}")
    private String wjzjm;

    @Value("${audit.wuhu.sheji.designHead}")
    private String whsjh;
    @Value("${audit.wuhu.sheji.designManager}")
    private String whsjm;
    @Value("${audit.wuhu.zaojia.costHead}")
    private String whzjh;
    @Value("${audit.wuhu.zaojia.costManager}")
    private String whzjm;

    /**
     * 设计页面展示
     *
     * @param pageVo
     * @return
     */
    public PageInfo<DesignInfo> designProjectSelect(DesignPageVo pageVo, UserInfo loginUser) {
        //分页原因所以放这
        MemberManage memberManage = memberManageDao.memberManageById();
        //分页插件
        PageHelper.startPage(pageVo.getPageNum(), pageVo.getPageSize());
        //展示集合
        List<DesignInfo> designInfos = new ArrayList<>();
        //前台获取的登录信息
        //如果设计状态为'未审核' 则展示当前用户需要审核的信息
        if ("1".equals(pageVo.getDesginStatus())) {
            //todo getLoginUser().getId()
            //则根据登录用户id展示于其身份对应的数据
            pageVo.setUserId(loginUser.getId());
//            pageVo.setUserId("user323");
            //如果当前用户是部门主管 或者 部门经理 则展示全部待审核信息
            if (wjsjh.equals(pageVo.getUserId()) || whsjh.equals(pageVo.getUserId()) || whsjm.equals(pageVo.getUserId())) {
                designInfos = designInfoMapper.designProjectSelect1(pageVo);
            } else {
                //普通员工则展示自己创建或者自己负责得
                designInfos = designInfoMapper.designProjectSelect(pageVo);
            }
            for (DesignInfo thisDesign : designInfos) {
                //获取当前待审核信息
                Example example = new Example(AuditInfo.class);
                example.createCriteria().andEqualTo("baseProjectId", thisDesign.getId())
                        .andEqualTo("auditResult", "0");
                AuditInfo auditInfo = auditInfoDao.selectOneByExample(example);
                if (auditInfo != null) {
                    if (auditInfo.getAuditorId() != null) {
                        //获得当前处理人
                        Example example1 = new Example(MemberManage.class);
                        example1.createCriteria().andEqualTo("id", auditInfo.getAuditorId());
                        MemberManage memberManage1 = memberManageDao.selectOneByExample(example1);
                        if (memberManage1 != null) {
                            thisDesign.setCurrentHandler(memberManage1.getMemberName());
                        } else {
                            thisDesign.setCurrentHandler("暂未审核");
                        }
                    }
                }
            }
            //集合填值
            if (designInfos.size() > 0) {
                for (DesignInfo designInfo : designInfos) {
                    //展示设计变更时间 如果为空展示 /
                    if (designInfo.getDesignChangeTime() == null || designInfo.getDesignChangeTime().equals("")) {
                        designInfo.setDesignChangeTime("/");
                    }

                    //根据设计人id 查询
                    MemberManage memberManage1 = memberManageDao.selectByPrimaryKey(designInfo.getDesigner());
                    //将id替换成姓名
                    if (memberManage1 != null) {
                        designInfo.setDesigner(memberManage1.getMemberName());
                    } else {
                        designInfo.setDesigner("-");
                    }

                    //根据地区判断相应的设计费 应付金额 实付金额
                    //如果为安徽
                    if (designInfo.getDistrict() != null) {
                        if (!designInfo.getDistrict().equals("4")) {
                            Example anhui = new Example(AnhuiMoneyinfo.class);
                            Example.Criteria c2 = anhui.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(anhui);
                            if (anhuiMoneyinfo != null) {
                                designInfo.setRevenue(anhuiMoneyinfo.getRevenue());
                                designInfo.setOfficialReceipts(anhuiMoneyinfo.getOfficialReceipts());
                                designInfo.setDisMoney(anhuiMoneyinfo.getRevenue());
                                designInfo.setPayTerm(anhuiMoneyinfo.getPayTerm());
                            }
                            //如果为吴江
                        } else {
                            Example wujiang = new Example(WujiangMoneyInfo.class);
                            Example.Criteria c2 = wujiang.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(wujiang);
                            if (wujiangMoneyInfo != null) {
                                designInfo.setRevenue(wujiangMoneyInfo.getRevenue());
                                designInfo.setOfficialReceipts(wujiangMoneyInfo.getOfficialReceipts());
                                designInfo.setDisMoney(wujiangMoneyInfo.getRevenue());
                                designInfo.setPayTerm(wujiangMoneyInfo.getPayTerm());
                            }
                        }
                    }
                    //获取预算表中的造价金额
                    Example example = new Example(Budgeting.class);
                    Example.Criteria c = example.createCriteria();
                    c.andEqualTo("baseProjectId", designInfo.getBaseProjectId());
                    Budgeting budgeting = budgetingMapper.selectOneByExample(example);
                    if (budgeting != null) {
                        designInfo.setAmountCost(budgeting.getAmountCost());
                    } else {
                        designInfo.setAmountCost(new BigDecimal(0));
                    }
                }
            }
            PageInfo<DesignInfo> designInfoPageInfo = new PageInfo<>(designInfos);
            return designInfoPageInfo;
        }

        //如果状态为出图中
        if ("2".equals(pageVo.getDesginStatus())) {
            //todo loginUser.getId()
            pageVo.setUserId(loginUser.getId());
            designInfos = designInfoMapper.designProjectSelect2(pageVo);
            if (designInfos != null) {
                if (designInfos.size() > 0) {
                    for (DesignInfo designInfo : designInfos) {
                        //展示设计变更时间 如果为空展示 /
                        if (designInfo.getDesignChangeTime() == null || designInfo.getDesignChangeTime().equals("")) {
                            designInfo.setDesignChangeTime("/");
                        }

                        //根据设计人id 查询
                        MemberManage memberManage1 = memberManageDao.selectByPrimaryKey(designInfo.getDesigner());
                        //将id替换成姓名
                        if (memberManage1 != null) {
                            designInfo.setDesigner(memberManage1.getMemberName());
                        } else {
                            designInfo.setDesigner("-");
                        }

                        //根据地区判断相应的设计费 应付金额 实付金额
                        //如果为安徽
                        if (designInfo.getDistrict() != null) {
                            if (!designInfo.getDistrict().equals("4")) {
                                Example anhui = new Example(AnhuiMoneyinfo.class);
                                Example.Criteria c2 = anhui.createCriteria();
                                c2.andEqualTo("baseProjectId", designInfo.getId());
                                AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(anhui);
                                if (anhuiMoneyinfo != null) {
                                    designInfo.setRevenue(anhuiMoneyinfo.getRevenue());
                                    designInfo.setOfficialReceipts(anhuiMoneyinfo.getOfficialReceipts());
                                    designInfo.setDisMoney(anhuiMoneyinfo.getRevenue());
                                    designInfo.setPayTerm(anhuiMoneyinfo.getPayTerm());
                                }
                                //如果为吴江
                            } else {
                                Example wujiang = new Example(WujiangMoneyInfo.class);
                                Example.Criteria c2 = wujiang.createCriteria();
                                c2.andEqualTo("baseProjectId", designInfo.getId());
                                WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(wujiang);
                                if (wujiangMoneyInfo != null) {
                                    designInfo.setRevenue(wujiangMoneyInfo.getRevenue());
                                    designInfo.setOfficialReceipts(wujiangMoneyInfo.getOfficialReceipts());
                                    designInfo.setDisMoney(wujiangMoneyInfo.getRevenue());
                                    designInfo.setPayTerm(wujiangMoneyInfo.getPayTerm());
                                }
                            }
                        }

                        //获取预算表中的造价金额
                        Example example = new Example(Budgeting.class);
                        Example.Criteria c = example.createCriteria();
                        c.andEqualTo("baseProjectId", designInfo.getBaseProjectId());
                        Budgeting budgeting = budgetingMapper.selectOneByExample(example);
                        if (budgeting != null) {
                            designInfo.setAmountCost(budgeting.getAmountCost());
                        } else {
                            designInfo.setAmountCost(new BigDecimal(0));
                        }
                    }
                }
            }
            PageInfo<DesignInfo> designInfoPageInfo = new PageInfo<>(designInfos);
            return designInfoPageInfo;
        }
        //如果为未通过
        if ("3".equals(pageVo.getDesginStatus())) {
            //将部门负责人传入
            pageVo.setAdminId(memberManage.getId());
            //todo loginUser.getId()
            pageVo.setUserId(loginUser.getId());
            designInfos = designInfoMapper.designProjectSelect3(pageVo);
            if (designInfos.size() > 0) {
                for (DesignInfo designInfo : designInfos) {

                    //根据设计人id 查询
                    MemberManage memberManage1 = memberManageDao.selectByPrimaryKey(designInfo.getDesigner());
                    //将id替换成姓名
                    if (memberManage1 != null) {
                        designInfo.setDesigner(memberManage1.getMemberName());
                    } else {
                        designInfo.setDesigner("-");
                    }

                    //判断当前项目是否为设计变更项目 如果是返回一个状态
                    Example changeExample = new Example(AuditInfo.class);
                    changeExample.createCriteria()
                            .andEqualTo("baseProjectId", designInfo.getId())
                            .andEqualTo("auditResult", "2")
                            .andEqualTo("changeFlag", "0");
                    AuditInfo auditInfo = auditInfoDao.selectOneByExample(changeExample);
                    if (auditInfo != null) {
                        //如果是变更项目返回一个状态
                        designInfo.setDesChangeFlag("1");
                    } else {
                        designInfo.setDesChangeFlag("0");
                    }
                    //展示设计变更时间 如果为空展示 /
                    if (designInfo.getDesignChangeTime() == null || designInfo.getDesignChangeTime().equals("")) {
                        designInfo.setDesignChangeTime("/");
                    }
                    //根据地区判断相应的设计费 应付金额 实付金额
                    //如果为安徽
                    if (designInfo.getDistrict() != null) {
                        if (!designInfo.getDistrict().equals("4")) {
                            Example anhui = new Example(AnhuiMoneyinfo.class);
                            Example.Criteria c2 = anhui.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(anhui);
                            if (anhuiMoneyinfo != null) {
                                designInfo.setRevenue(anhuiMoneyinfo.getRevenue());
                                designInfo.setOfficialReceipts(anhuiMoneyinfo.getOfficialReceipts());
                                designInfo.setDisMoney(anhuiMoneyinfo.getRevenue());
                                designInfo.setPayTerm(anhuiMoneyinfo.getPayTerm());
                            }
                            //如果为吴江
                        } else {
                            Example wujiang = new Example(WujiangMoneyInfo.class);
                            Example.Criteria c2 = wujiang.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(wujiang);
                            if (wujiangMoneyInfo != null) {
                                designInfo.setRevenue(wujiangMoneyInfo.getRevenue());
                                designInfo.setOfficialReceipts(wujiangMoneyInfo.getOfficialReceipts());
                                designInfo.setDisMoney(wujiangMoneyInfo.getRevenue());
                                designInfo.setPayTerm(wujiangMoneyInfo.getPayTerm());
                            }
                        }
                    }
                    //获取预算表中的造价金额
                    Example example = new Example(Budgeting.class);
                    Example.Criteria c = example.createCriteria();
                    c.andEqualTo("baseProjectId", designInfo.getBaseProjectId());
                    Budgeting budgeting = budgetingMapper.selectOneByExample(example);
                    if (budgeting != null) {
                        designInfo.setAmountCost(budgeting.getAmountCost());
                    } else {
                        designInfo.setAmountCost(new BigDecimal(0));
                    }
                }
            }
        }
        //如果为已完成
        if ("4".equals(pageVo.getDesginStatus())) {
            //已完成不分层级所以全部展示
            designInfos = designInfoMapper.designProjectSelect4(pageVo);
            if (designInfos.size() > 0) {
                for (DesignInfo designInfo : designInfos) {
                    //展示设计变更时间 如果为空展示 /
                    if (designInfo.getDesignChangeTime() == null || designInfo.getDesignChangeTime().equals("")) {
                        designInfo.setDesignChangeTime("/");
                    }
                    //根据设计人id 查询
                    MemberManage memberManage1 = memberManageDao.selectByPrimaryKey(designInfo.getDesigner());
                    //将id替换成姓名
                    if (memberManage1 != null) {
                        designInfo.setDesigner(memberManage1.getMemberName());
                    } else {
                        designInfo.setDesigner("-");
                    }

                    //归属按钮展示
                    //todo loginUser.getId();
                    String loginUserId = loginUser.getId();

                    //如果当前登入人等于创建人
                    if (designInfo.getFounderId().equals(loginUserId)) {
                        //说明当前项目是创建人项目
                        designInfo.setAscriptionFlag("1");
                    }
                    //根据地区判断相应的设计费 应付金额 实付金额
                    //如果为安徽
                    if (designInfo.getDistrict() != null) {
                        if (!designInfo.getDistrict().equals("4")) {
                            Example anhui = new Example(AnhuiMoneyinfo.class);
                            Example.Criteria c2 = anhui.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(anhui);
                            if (anhuiMoneyinfo != null) {
                                designInfo.setRevenue(anhuiMoneyinfo.getRevenue());
                                designInfo.setOfficialReceipts(anhuiMoneyinfo.getOfficialReceipts());
                                designInfo.setDisMoney(anhuiMoneyinfo.getRevenue());
                                designInfo.setPayTerm(anhuiMoneyinfo.getPayTerm());
                            }
                            //如果为吴江
                        } else {
                            Example wujiang = new Example(WujiangMoneyInfo.class);
                            Example.Criteria c2 = wujiang.createCriteria();
                            c2.andEqualTo("baseProjectId", designInfo.getId());
                            WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(wujiang);
                            if (wujiangMoneyInfo != null) {
                                designInfo.setRevenue(wujiangMoneyInfo.getRevenue());
                                designInfo.setOfficialReceipts(wujiangMoneyInfo.getOfficialReceipts());
                                designInfo.setDisMoney(wujiangMoneyInfo.getRevenue());
                                designInfo.setPayTerm(wujiangMoneyInfo.getPayTerm());
                            }
                        }
                    }
                    //获取预算表中的造价金额
                    Example example = new Example(Budgeting.class);
                    Example.Criteria c = example.createCriteria();
                    c.andEqualTo("baseProjectId", designInfo.getBaseProjectId());
                    c.andEqualTo("delFlag", "0");
                    Budgeting budgeting = budgetingMapper.selectOneByExample(example);
                    if (budgeting != null) {
                        designInfo.setAmountCost(budgeting.getAmountCost());
                    } else {
                        designInfo.setAmountCost(new BigDecimal(0));
                    }
                }
            }
        }else{
            //查询全部
            //查找集团领导
            Example admin = new Example(MemberManage.class);
            Example.Criteria adminc = admin.createCriteria();
            adminc.andEqualTo("memberRoleId", "2");
            MemberManage boss = memberManageDao.selectOneByExample(admin);
            //判断当前人是否为部门领导 如果是 则展示所有数据
            if (boss.getId().equals(loginUser.getId())) {
                pageVo.setUserId("");
                pageVo.setAdminId("");
            } else {
                //将部门负责人传入
                pageVo.setAdminId(memberManage.getId());
                //todo loginUser.getId()
                pageVo.setUserId(loginUser.getId());
            }
            designInfos = designInfoMapper.designProjectSelect3(pageVo);
            for (DesignInfo designInfo : designInfos) {
                //展示设计变更时间 如果为空展示 /
                if (designInfo.getDesignChangeTime() == null || designInfo.getDesignChangeTime().equals("")) {
                    designInfo.setDesignChangeTime("/");
                }

                //根据设计人id 查询
                MemberManage memberManage1 = memberManageDao.selectByPrimaryKey(designInfo.getDesigner());
                //将id替换成姓名
                if (memberManage1 != null) {
                    designInfo.setDesigner(memberManage1.getMemberName());
                } else {
                    designInfo.setDesigner("-");
                }

                //根据地区判断相应的设计费 应付金额 实付金额
                //如果为安徽
                if (designInfo.getDistrict() != null) {
                    if (!designInfo.getDistrict().equals("4")) {
                        Example anhui = new Example(AnhuiMoneyinfo.class);
                        Example.Criteria c2 = anhui.createCriteria();
                        c2.andEqualTo("baseProjectId", designInfo.getId());
                        AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(anhui);
                        if (anhuiMoneyinfo != null) {
                            designInfo.setRevenue(anhuiMoneyinfo.getRevenue());
                            designInfo.setOfficialReceipts(anhuiMoneyinfo.getOfficialReceipts());
                            designInfo.setDisMoney(anhuiMoneyinfo.getRevenue());
                            designInfo.setPayTerm(anhuiMoneyinfo.getPayTerm());
                        }
                        //如果为吴江
                    } else {
                        Example wujiang = new Example(WujiangMoneyInfo.class);
                        Example.Criteria c2 = wujiang.createCriteria();
                        c2.andEqualTo("baseProjectId", designInfo.getId());
                        WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(wujiang);
                        if (wujiangMoneyInfo != null) {
                            designInfo.setRevenue(wujiangMoneyInfo.getRevenue());
                            designInfo.setOfficialReceipts(wujiangMoneyInfo.getOfficialReceipts());
                            designInfo.setDisMoney(wujiangMoneyInfo.getRevenue());
                            designInfo.setPayTerm(wujiangMoneyInfo.getPayTerm());
                        }
                    }
                }
                //获取预算表中的造价金额
                Example example = new Example(Budgeting.class);
                Example.Criteria c = example.createCriteria();
                c.andEqualTo("baseProjectId", designInfo.getBaseProjectId());
                Budgeting budgeting = budgetingMapper.selectOneByExample(example);
                if (budgeting != null) {
                    designInfo.setAmountCost(budgeting.getAmountCost());
                } else {
                    designInfo.setAmountCost(new BigDecimal(0));
                }
            }
        }
        PageInfo<DesignInfo> designInfoPageInfo = new PageInfo<>(designInfos);
        return designInfoPageInfo;
    }

    public Double wujiangMoney(WujiangMoneyInfo wujiangMoneyInfo) {
        BigDecimal cost = wujiangMoneyInfo.getCost(); // 造价费用
        BigDecimal designRate = wujiangMoneyInfo.getDesignRate(); //设计费率
        BigDecimal preferentialPolicy = wujiangMoneyInfo.getPreferentialPolicy();//优惠政策
        //造价*设计费
        BigDecimal CostAndDesignRate = cost.multiply(designRate);
        //上述积 * 政策
        BigDecimal multiply = CostAndDesignRate.multiply(preferentialPolicy);
        return multiply.doubleValue();
    }

    public Double anhuiMoney(AnhuiMoneyinfo anhuiMoneyinfo) {
        //管道费用
        BigDecimal pipelineCost = anhuiMoneyinfo.getPipelineCost();
        //泵房费用
        BigDecimal pumpRoomCost = anhuiMoneyinfo.getPumpRoomCost();

        //如果未填写泵房 则只计算管道费用 (管道不计算bim)
        if (pumpRoomCost == null || "".equals(pumpRoomCost)) {
            //如果小于200万
            if (pipelineCost.compareTo(new BigDecimal("2000000")) == -1) {
                //插值法则  低于200万 直接乘以0.0575 * 优惠政策
                BigDecimal PipelineCost = pipelineCost.multiply(new BigDecimal(0.0575));
                BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PipelineCost);
                return PreferentialPolicy.doubleValue();
            }

            //高于10000万
            if (pipelineCost.compareTo(new BigDecimal("100000000000")) == 1) {
                //插值法则   直接乘以0.016 * 优惠政策
                BigDecimal PipelineCost = pipelineCost.multiply(new BigDecimal(0.016));
                BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PipelineCost);
                return PreferentialPolicy.doubleValue();
            }
            //管道插值法
            BigDecimal pipelineCostCount = this.pipelineCostCount(anhuiMoneyinfo);
            BigDecimal professionalAdjustmentFactor = anhuiMoneyinfo.getProfessionalAdjustmentFactor().multiply(pipelineCostCount);
            BigDecimal complexAdjustmentFactor = anhuiMoneyinfo.getComplexAdjustmentFactor().multiply(professionalAdjustmentFactor);
            BigDecimal multiply = anhuiMoneyinfo.getPreferentialPolicy().multiply(complexAdjustmentFactor);
            return multiply.doubleValue() * 10000;
        } else if (pipelineCost == null || "".equals(pipelineCost)) {
            //如果小于200万
            if (pumpRoomCost.compareTo(new BigDecimal("2000000")) == -1) {
                //插值法则  低于200万 直接乘以0.0575 * 优惠政策
                BigDecimal PumpRoomCost = pumpRoomCost.multiply(new BigDecimal(0.0575));
                BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PumpRoomCost);
                return PreferentialPolicy.doubleValue();
            }

            //高于10000万
            if (pumpRoomCost.compareTo(new BigDecimal("100000000000")) == 1) {
                //插值法则   直接乘以0.016 * 优惠政策
                BigDecimal PumpRoomCost = pumpRoomCost.multiply(new BigDecimal(0.016));
                BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PumpRoomCost);
                return PreferentialPolicy.doubleValue();
            }
            //泵房插值法
            BigDecimal pumpRoomCostCount = this.pumpRoomCostCount(anhuiMoneyinfo);
            BigDecimal professionalAdjustmentFactor = anhuiMoneyinfo.getProfessionalAdjustmentFactor().multiply(pumpRoomCostCount);
            BigDecimal complexAdjustmentFactor = anhuiMoneyinfo.getComplexAdjustmentFactor().multiply(professionalAdjustmentFactor);
            BigDecimal multiply = anhuiMoneyinfo.getPreferentialPolicy().multiply(complexAdjustmentFactor);
            return multiply.doubleValue() * 10000;
        }
        //水管算法
        BigDecimal pipelineCostCount = this.pipelineCostCount(anhuiMoneyinfo);
        //泵房算法
        BigDecimal pumpRoomCostCount = this.pumpRoomCostCount(anhuiMoneyinfo);

        // 当泵房 和 管道都填写时
        //泵房 * bim
        BigDecimal pipelineCostCountBim = pumpRoomCostCount.multiply(anhuiMoneyinfo.getBim());
        //泵房Bim + 管道费用
        BigDecimal add = pipelineCostCount.add(pipelineCostCountBim);
        //复杂
        BigDecimal multiply1 = anhuiMoneyinfo.getProfessionalAdjustmentFactor().multiply(add);
        //专业
        BigDecimal multiply2 = anhuiMoneyinfo.getComplexAdjustmentFactor().multiply(multiply1);
        //优惠
        BigDecimal multiply3 = anhuiMoneyinfo.getPreferentialPolicy().multiply(multiply2);
        BigDecimal multiply4 = multiply3.multiply(new BigDecimal(10000));
        return multiply4.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        return multiply3.doubleValue() * 10000;
    }

    /**
     * 管道算法
     *
     * @return
     */
    public BigDecimal pipelineCostCount(AnhuiMoneyinfo anhuiMoneyinfo) {
        //管道费用
        BigDecimal pipelineCost = anhuiMoneyinfo.getPipelineCost();
        //传输过来的参数值/10000
        pipelineCost = pipelineCost.divide(new BigDecimal(10000));

        //如果小于200万
        if (pipelineCost.compareTo(new BigDecimal("200")) == -1) {
            //插值法则  低于200万 直接乘以0.0575 * 优惠政策
            BigDecimal PipelineCost = pipelineCost.multiply(new BigDecimal(0.0575));
            BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PipelineCost);
            return PreferentialPolicy;
        }

        //高于10000000万
        if (pipelineCost.compareTo(new BigDecimal("10000000")) == 1) {
            //插值法则   直接乘以0.016 * 优惠政策
            BigDecimal PipelineCost = pipelineCost.multiply(new BigDecimal(0.016));
            BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PipelineCost);
            return PreferentialPolicy;
        }

        //如果值为 表的区间内 则使用插值法计算
        List<MunicipalNgineerDesign> municipalNgineerDesigns = municipalNgineerDesignMapper.designMoney(pipelineCost);
        if (municipalNgineerDesigns.size() > 1) {
            BigDecimal dis1 = new BigDecimal(municipalNgineerDesigns.get(0).getDesignBasicCost());
            BigDecimal dis2 = new BigDecimal(municipalNgineerDesigns.get(1).getDesignBasicCost());

            BigDecimal project1 = new BigDecimal(municipalNgineerDesigns.get(0).getProjectCost());
            BigDecimal project2 = new BigDecimal(municipalNgineerDesigns.get(1).getProjectCost());

            //设计费差
            BigDecimal DisSubtract = dis1.subtract(dis2);
            //工程差
            BigDecimal ProjectSubtract = project1.subtract(project2);
            //插值
            BigDecimal divide = DisSubtract.divide(ProjectSubtract, 6, BigDecimal.ROUND_HALF_UP);
            //管道差
            BigDecimal prjectMoney = pipelineCost.subtract(project2);
            //管道插值 * 插值
            BigDecimal multiply = divide.multiply(prjectMoney);
            //最后管道结果
            BigDecimal add = multiply.add(dis2);
            return add;
        }
        Example example = new Example(MunicipalNgineerDesign.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("projectCost", pipelineCost);
        MunicipalNgineerDesign municipalNgineerDesign = municipalNgineerDesignMapper.selectOneByExample(example);
        //返回应收金额
        return new BigDecimal(municipalNgineerDesign.getDesignBasicCost());
    }

    /**
     * 泵房算法
     *
     * @param anhuiMoneyinfo
     * @return
     */
    public BigDecimal pumpRoomCostCount(AnhuiMoneyinfo anhuiMoneyinfo) {
        //泵房费用
        BigDecimal pumpRoomCost = anhuiMoneyinfo.getPumpRoomCost();
        //传输过来的参数值/10000
        pumpRoomCost = pumpRoomCost.divide(new BigDecimal(10000));

        //如果小于200万
        if (pumpRoomCost.compareTo(new BigDecimal("200")) == -1) {
            //插值法则  低于200万 直接乘以0.0575 * 优惠政策
            BigDecimal PumpRoomCost = pumpRoomCost.multiply(new BigDecimal(0.0575));
            BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PumpRoomCost);
            return PreferentialPolicy;
        }

        //高于10000万
        if (pumpRoomCost.compareTo(new BigDecimal("10000000")) == 1) {
            //插值法则   直接乘以0.016 * 优惠政策
            BigDecimal PumpRoomCost = pumpRoomCost.multiply(new BigDecimal(0.016));
            BigDecimal PreferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(PumpRoomCost);
            return PreferentialPolicy;
        }
        //如果值为 表的区间内 则使用插值法计算
        List<MunicipalNgineerDesign> municipalNgineerDesigns = municipalNgineerDesignMapper.designMoney(pumpRoomCost);
        if (municipalNgineerDesigns.size() > 1) {
            BigDecimal dis1 = new BigDecimal(municipalNgineerDesigns.get(0).getDesignBasicCost());
            BigDecimal dis2 = new BigDecimal(municipalNgineerDesigns.get(1).getDesignBasicCost());

            BigDecimal project1 = new BigDecimal(municipalNgineerDesigns.get(0).getProjectCost());
            BigDecimal project2 = new BigDecimal(municipalNgineerDesigns.get(1).getProjectCost());

            //设计费差
            BigDecimal DisSubtract = dis1.subtract(dis2);
            //工程差
            BigDecimal ProjectSubtract = project1.subtract(project2);
            //插值
            BigDecimal divide = DisSubtract.divide(ProjectSubtract, 6, BigDecimal.ROUND_HALF_UP);
            //管道差
            BigDecimal prjectMoney = pumpRoomCost.subtract(project2);
            //管道插值 * 插值
            BigDecimal multiply = divide.multiply(prjectMoney);
            //最后管道结果
            BigDecimal add = multiply.add(dis2);
            //乘以复杂
            BigDecimal complexAdjustmentFactor = anhuiMoneyinfo.getComplexAdjustmentFactor().multiply(add);
            //乘以优惠
            BigDecimal preferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(complexAdjustmentFactor);
            //乘以专业
            BigDecimal professionalAdjustmentFactor = anhuiMoneyinfo.getProfessionalAdjustmentFactor().multiply(preferentialPolicy);
            //乘以bim
            BigDecimal bim = anhuiMoneyinfo.getBim().multiply(professionalAdjustmentFactor);
//            return multiply3.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            return bim;
        }
        Example example = new Example(MunicipalNgineerDesign.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("projectCost", pumpRoomCost);
        MunicipalNgineerDesign municipalNgineerDesign = municipalNgineerDesignMapper.selectOneByExample(example);
        //返回应收金额
        Double designBasicCost = municipalNgineerDesign.getDesignBasicCost();
        //乘以复杂
        BigDecimal complexAdjustmentFactor = anhuiMoneyinfo.getComplexAdjustmentFactor().multiply(new BigDecimal(designBasicCost));
        //乘以优惠
        BigDecimal preferentialPolicy = anhuiMoneyinfo.getPreferentialPolicy().multiply(complexAdjustmentFactor);
        //乘以专业
        BigDecimal multiply1 = anhuiMoneyinfo.getProfessionalAdjustmentFactor().multiply(preferentialPolicy);
        //乘以bim
        BigDecimal bim = anhuiMoneyinfo.getBim().multiply(multiply1);
//            return bim.doubleValue() * 10000;
        return bim;
    }

    /**
     * 项目合并
     *
     * @param mergeName
     * @param mergeNum
     * @param id
     */

    public boolean mergeProject(String mergeName, String mergeNum, String id) {
        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(id);
        if (designInfo != null) {
            BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());
            if (baseProject != null) {
                //如果项目名称 与 项目编号 与数据库一致 则为主表 X=虚拟编号     是否为主表,是否删除状态
                if (baseProject.getProjectName().equals(mergeName) && baseProject.getProjectNum().equals(mergeNum)) {
                    String x = "x" + mergeNum;
                    projectMapper.updataMerga(x, "0", baseProject.getId(), "0");
                } else {
                    String x = "x" + mergeNum;
                    projectMapper.updataMerga(x, "1", baseProject.getId(), "1");
                }
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 合并列表展示
     *
     * @return
     */
    public DesignInfo mergeProjectList(String id) {
        //根据id查找 合并信息
        DesignInfo designInfo = designInfoMapper.designProjectSelectOne(id);
        return designInfo;
    }

    /**
     * 更新成非主表
     */
    public void updateMergeProject0(String id) {
        projectMapper.updateMergeProject0(id);
    }

    /**
     * 跟新为主表
     *
     * @param id
     */
    public void updateMergeProject1(String id) {
        projectMapper.updateMergeProject1(id);
    }

    /**
     * 批量审核
     *
     * @param id
     * @param auditInfo
     */
    public void batchAudit(String id, AuditInfo auditInfo, UserInfo loginUser) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTime = simpleDateFormat.format(new Date());
        //当前登录id
        //todo loginUser.getId();
        //todo loginUser.getCompanyId();
        String loginUserId = loginUser.getId();
        String companyId = loginUser.getCompanyId();
        //获取当前登录成员
        MemberManage memberManage = memberManageDao.selectByPrimaryKey(loginUserId);
        //根据id查询设计信息
        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(id);
        //根据设计id查询基本信息
        BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());
        //获取创建项目用户
        MemberManage createMember = memberManageDao.selectByPrimaryKey(baseProject.getFounderId());
        //根据外键 和 互审人id 查询审核信息
        Example example = new Example(AuditInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        c.andEqualTo("auditorId", loginUserId);
        c.andEqualTo("auditResult", "0"); //设计变更相关
        //当前审核信息
        AuditInfo auditInfo2 = auditInfoDao.selectOneByExample(example);

        if (memberManage != null) {
            //安徽设计员 需要进行三次审核
            if ("1".equals(createMember.getWorkType())) {
                if (memberManage.getId().equals(whsjh)) {
                    //说明是部门领导审核 二审
                    //如果部门领导选择通过 审核类型改为二审通过(1)
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //如果审核通过 需要将添加一条三级领导审核信息
                        String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
                        AuditInfo auditInfo1 = new AuditInfo();
                        //添加一个id
                        auditInfo1.setId(auditInfouuid);
                        //添加外键
                        auditInfo1.setBaseProjectId(designInfo.getId());
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //改为设计变更三审待审核
                            auditInfo1.setAuditType("5");
                            auditInfo1.setChangeFlag("0");
                        } else {
                            //审核类型为三审待审核(领导审核)
                            auditInfo1.setAuditType("4");
                            auditInfo1.setChangeFlag("1");
                        }
                        //审核结果 结果待审核(领导审核后变为已审核)
                        auditInfo1.setAuditResult("0");
                        //赋值安徽设计部门负责人
                        auditInfo1.setAuditorId(whsjm); //部门经理审核
                        //添加创建时间
                        auditInfo1.setCreateTime(createTime);
                        //添加创建人id
                        auditInfo1.setFounderId(loginUserId);
                        //添加公司id
                        auditInfo1.setCompanyId(companyId);
                        //状态正常
                        auditInfo1.setStatus("0");
                        //将新的领导信息添加到审核表中
                        auditInfoDao.insert(auditInfo1);

                        //修改上一个审核人状态
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更二审
                            auditInfo2.setAuditType("3");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 二审
                            auditInfo2.setAuditType("1");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);

                    } else if ("2".equals(auditInfo.getAuditResult())) {
                        //如果领导选择未通过 审核类型改为二审(1) 同时项目状态变为未通过
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更二审
                            auditInfo2.setAuditType("3");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 二审
                            auditInfo2.setAuditType("1");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setUpdateTime(createTime);
                        baseProject.setDesginStatus("3");
                        auditInfo2.setAuditTime(createTime);
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    }
                } else if (memberManage.getId().equals(whsjm)) {
                    String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    //说明是部门经理审核 三审
                    //如果为通过 则从待审核状态变为已完成 如果为未通过则状态改为未通过
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //如果领导选择通过 审核类型改为三审(4)  同时项目状态变为已完成
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更三审
                            auditInfo2.setAuditType("5");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 三审
                            auditInfo2.setAuditType("4");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        baseProject.setDesginStatus("4");
                        auditInfo2.setAuditTime(createTime);
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);

                        // 加入委外信息表
                        OutSource outSource = new OutSource();
                        outSource.setId(UUID.randomUUID().toString().replaceAll("-",""));
                        // 如果有委外金额就加入委外信息表
                        if ("0".equals(designInfo.getOutsource())){
                            outSource.setOutMoney(designInfo.getOutsourceMoney().toString());
                        }else {
                            outSource.setOutMoney("0");
                        }
                        outSource.setDistrict(baseProject.getDistrict());
                        outSource.setDept("1"); //1.设计 2.造价
                        outSource.setDelFlag("0"); //0.正常 1.删除
                        outSource.setOutType("1"); // 设计委外金额
                        outSource.setBaseProjectId(baseProject.getId()); //基本信息表外键
                        outSource.setProjectNum(designInfo.getId()); //设计信息外键
                        outSource.setCreateTime(data);
                        outSource.setUpdateTime(data);
                        outSource.setFounderId(designInfo.getFounderId()); //项目创建人
                        outSource.setFounderCompanyId(designInfo.getCompanyId()); //公司
                        outSourceMapper.insertSelective(outSource);

                    } else if ("2".equals(auditInfo.getAuditResult())) {
                        //如果领导选择通过 审核类型改为三审(4) 同时该项目负责人变为部门经理 同时项目状态变为未通过
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更三审
                            auditInfo2.setAuditType("5");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 三审
                            auditInfo2.setAuditType("4");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setAuditTime(createTime);
                        auditInfo2.setUpdateTime(createTime);
                        baseProject.setDesginStatus("3");
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    }
                } else {
                    //普通互审人 一审
                    //如果是通过
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //如果审核通过 需要将添加一条领导审核信息
                        String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
                        AuditInfo auditInfo1 = new AuditInfo();
                        //添加一个id
                        auditInfo1.setId(auditInfouuid);
                        //添加外键
                        auditInfo1.setBaseProjectId(designInfo.getId());
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //根据设计外键删除之前得设计变更信息
                            this.deleteDesChangeAudit(designInfo.getId());
                            //改为设计变更二审待审核
                            auditInfo1.setAuditType("3");
                            auditInfo1.setChangeFlag("0");
                        } else {
                            //审核类型为二审待审核(领导审核)
                            auditInfo1.setAuditType("1");
                            auditInfo1.setChangeFlag("1");
                        }
                        //审核结果 结果待审核(领导审核后变为已审核)
                        auditInfo1.setAuditResult("0");
                        //赋值安徽设计部门负责人
                        auditInfo1.setAuditorId(whsjh);
                        //添加创建时间
                        auditInfo1.setCreateTime(createTime);
                        //添加创建人id
                        auditInfo1.setFounderId(loginUserId);
                        //添加公司id
                        auditInfo1.setCompanyId(companyId);
                        //状态正常
                        auditInfo1.setStatus("0");
                        //将新的领导信息添加到审核表中
                        auditInfoDao.insert(auditInfo1);

                        //更改之前普通互审人得状态
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更一审
                            auditInfo2.setAuditType("2");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 一审
                            auditInfo2.setAuditType("0");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setAuditTime(createTime);
                        auditInfo2.setUpdateTime(createTime);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    } else {
                        //如果是未通过则
                        //基本信息状态改为未通过
                        baseProject.setDesginStatus("3");
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更一审未通过
                            auditInfo2.setAuditType("2");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 一审未通过
                            auditInfo2.setAuditType("0");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    }
                }
            }

            //吴江设计员 需要进行两次审核 (吴江设计领导和经理是一个人)
            if ("2".equals(createMember.getWorkType())) {
                if (memberManage.getId().equals(wjsjh)) {
                    //说明是部门领导审核 三审
                    //如果为通过 则从待审核状态变为已完成 如果为未通过则状态改为未通过
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更三审通过
                            auditInfo2.setAuditType("5");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //如果领导选择通过 审核类型改为三审(4)
                            auditInfo2.setAuditType("4");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        baseProject.setDesginStatus("4");
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                        String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        OutSource outSource = new OutSource();
                        outSource.setId(UUID.randomUUID().toString().replaceAll("-",""));
                        if ("0".equals(designInfo.getOutsource())){
                            outSource.setOutMoney(designInfo.getOutsourceMoney().toString());
                        }else{
                            outSource.setOutMoney("0");
                        }
                        outSource.setDistrict(baseProject.getDistrict());
                        outSource.setDept("1"); //1.设计 2.造价
                        outSource.setDelFlag("0"); //0.正常 1.删除
                        outSource.setOutType("1"); // 设计委外金额
                        outSource.setBaseProjectId(baseProject.getId()); //基本信息表外键
                        outSource.setProjectNum(designInfo.getId()); //设计信息外键
                        outSource.setCreateTime(data);
                        outSource.setUpdateTime(data);
                        outSource.setFounderId(designInfo.getFounderId()); //项目创建人
                        outSource.setFounderCompanyId(designInfo.getCompanyId()); //公司
                        outSourceMapper.insertSelective(outSource);

                    } else if ("2".equals(auditInfo.getAuditResult())) {
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更三审通过
                            auditInfo2.setAuditType("5");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核类型改为三审(4)
                            auditInfo2.setAuditType("4");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        baseProject.setDesginStatus("3");
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    }
                } else {
                    //普通互审人 一审
                    //如果是通过
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //如果审核通过 需要将添加一条领导审核信息
                        String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
                        AuditInfo auditInfo1 = new AuditInfo();
                        //添加一个id
                        auditInfo1.setId(auditInfouuid);
                        //添加外键
                        auditInfo1.setBaseProjectId(designInfo.getId());
                        //审核类型为三审(领导审核)
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //根据设计外键删除之前得设计变更信息
                            this.deleteDesChangeAudit(designInfo.getId());
                            //改为设计变更三审审待审核
                            auditInfo1.setAuditType("5");
                            auditInfo1.setChangeFlag("0");
                        } else {
                            //审核类型为三审待审核(领导审核)
                            auditInfo1.setAuditType("4");
                            auditInfo1.setChangeFlag("1");
                        }
                        //审核结果 结果待审核(领导审核后变为已审核)
                        auditInfo1.setAuditResult("0");
                        //赋值吴江设计部门负责人
                        auditInfo1.setAuditorId(wjsjh);
                        //添加创建时间
                        auditInfo1.setCreateTime(createTime);
                        //添加创建人id
                        auditInfo1.setFounderId(loginUserId);
                        //添加公司id
                        auditInfo1.setCompanyId(companyId);
                        //状态正常
                        auditInfo1.setStatus("0");
                        //将新的领导信息添加到审核表中
                        auditInfoDao.insert(auditInfo1);

                        //更改普通互审人得状态
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更一审
                            auditInfo2.setAuditType("2");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 一审
                            auditInfo2.setAuditType("0");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    } else {
                        //如果是未通过则
                        //基本信息状态改为未通过
                        baseProject.setDesginStatus("3");
                        projectMapper.updateByPrimaryKeySelective(baseProject);
                        //审核信息写入
                        //如果当前项目为设计变更项目
                        if ("0".equals(auditInfo2.getChangeFlag())) {
                            //审核信息写入 变更一审未通过
                            auditInfo2.setAuditType("2");
                            auditInfo2.setChangeFlag("0");
                        } else {
                            //审核信息写入 一审未通过
                            auditInfo2.setAuditType("0");
                            auditInfo2.setChangeFlag("1");
                        }
                        auditInfo2.setAuditResult(auditInfo.getAuditResult());
                        auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
                        auditInfo2.setUpdateTime(createTime);
                        auditInfo2.setAuditTime(createTime);
                        auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
                    }
                }
            }
        }
        //消息通知
        String username = loginUser.getUsername();
        String projectName = baseProject.getProjectName();
        String name = memberManage.getMemberName();
        if ("1".equals(auditInfo.getAuditResult())) {
            MessageVo messageVo = new MessageVo();
            messageVo.setId("A05");
            messageVo.setUserId(loginUserId);
            messageVo.setType("1"); //通知
            messageVo.setTitle("您有一个设计项目已通过！");
            messageVo.setDetails(username + "您好！您提交的【" + projectName + "】的设计项目【" + name + "】已审批通过！");
            messageService.sendOrClose(messageVo);
        }
        if ("2".equals(auditInfo.getAuditResult())) {
            MessageVo messageVo1 = new MessageVo();
            messageVo1.setId("A05");
            messageVo1.setType("1"); // 通知
            messageVo1.setUserId(loginUserId);
            messageVo1.setTitle("您有一个设计项目未通过！");
            messageVo1.setDetails(username + "您好！您提交的【" + projectName + "】的设计项目【" + name + "】已未通过，请查看详情！");
            //调用消息Service
            messageService.sendOrClose(messageVo1);
        }

//        if(memberManage!=null){
//            if("4".equals(memberManage.getMemberRoleId())&&"1".equals(memberManage.getDepAdmin())){
//                //如果为通过 则从待审核状态变为已完成 如果为未通过则状态改为未通过
//                if("1".equals(auditInfo.getAuditResult())){
//                    //如果领导选择通过 审核类型改为二审(1) 同时项目状态变为已完成
//                    auditInfo2.setAuditType("1");
//                    auditInfo2.setAuditResult(auditInfo.getAuditResult());
//                    auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
//                    auditInfo2.setUpdateTime(createTime);
//                    baseProject.setDesginStatus("4");
//                    projectMapper.updateByPrimaryKeySelective(baseProject);
//                    auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
//                }else if("2".equals(auditInfo.getAuditResult())){
//                    //如果领导选择未通过 审核类型改为二审(1) 同时项目状态变为未通过
//                    auditInfo2.setAuditType("1");
//                    auditInfo2.setAuditResult(auditInfo.getAuditResult());
//                    auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
//                    auditInfo2.setUpdateTime(createTime);
//                    baseProject.setDesginStatus("3");
//                    projectMapper.updateByPrimaryKeySelective(baseProject);
//                    auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
//                }
//            }
//            //不是负责人说明是普通互审人
//            else if("2".equals(auditInfo.getAuditResult())){
//                //如果是未通过则
//                //基本信息状态改为未通过
//                baseProject.setDesginStatus("3");
//                //审核信息写入
//                auditInfo2.setAuditResult(auditInfo.getAuditResult());
//                auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
//                auditInfo2.setUpdateTime(createTime);
//                projectMapper.updateByPrimaryKeySelective(baseProject);
//                auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
//            }else if("1".equals(auditInfo.getAuditResult())){
//                //获取设计部门负责人
//                Example example2 = new Example(MemberManage.class);
//                Example.Criteria criteria = example2.createCriteria();
//                criteria.andEqualTo("depId","1");
//                criteria.andEqualTo("depAdmin","1");
//                MemberManage depAdmin = memberManageDao.selectOneByExample(example2);
//                //如果审核通过 需要将添加一条领导审核信息
//                String auditInfouuid = UUID.randomUUID().toString().replaceAll("-","");
//                AuditInfo auditInfo1 = new AuditInfo();
//                //添加一个id
//                auditInfo1.setId(auditInfouuid);
//                //添加外键
//                auditInfo1.setBaseProjectId(designInfo.getId());
//                //审核类型为二审(领导审核)
//                auditInfo1.setAuditType("1");
//                //审核结果 结果待审核(领导审核后变为已审核)
//                auditInfo1.setAuditResult("0");
//                //赋值审核人id(领导)
//                auditInfo1.setAuditorId(depAdmin.getId());
//                //添加创建时间
//                auditInfo1.setCreateTime(createTime);
//                //添加创建人id
//                auditInfo1.setFounderId(loginUser.getId());
//                //添加公司id
//                auditInfo1.setCompanyId(loginUser.getCompanyId());
//                //状态正常
//                auditInfo1.setStatus("0");
//                //将新的领导信息添加到审核表中
//                auditInfoDao.insert(auditInfo1);
//
//                //更改普通互审人得状态
//                auditInfo2.setAuditResult(auditInfo.getAuditResult());
//                auditInfo2.setAuditType("0");
//                auditInfo2.setAuditOpinion(auditInfo.getAuditOpinion());
//                auditInfo2.setUpdateTime(createTime);
//                auditInfoDao.updateByPrimaryKeySelective(auditInfo2);
//            }
//        }
    }

    /**
     * 删除旧的设计变更审核信息
     */
    public void deleteDesChangeAudit(String id) {
        //根据外键和设计状态查询设计变更审核
        Example example = new Example(AuditInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        c.andEqualTo("changeFlag", "0"); //设计变更相关
        List<AuditInfo> auditInfos = auditInfoDao.selectByExample(example);
        for (AuditInfo auditInfo : auditInfos) {
            auditInfoDao.deleteChangeOld(auditInfo.getId());
        }
    }

    /**
     * 合并状态还原
     */
    public void reduction(String id) {
        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(id);
        if (designInfo != null) {
            BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());
            if (baseProject != null) {
                projectMapper.reduction(baseProject.getVirtualCode());
            }
        }
    }

    /**
     * 删除设计项目
     *
     * @param id
     */
    public void deleteProject(String id) {
        designInfoMapper.deleteProject(id);
        designChangeInfoMapper.deleteProject(id);
        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(id);
        projectMapper.deleteProject(designInfo.getBaseProjectId());
    }


    @Autowired
    private RemindSetMapper remindSetMapper;

    @Autowired
    private MessageService messageService;

    /**
     * 提交设计项目
     *
     * @param projectVo
     */
    public void disProjectSubmit(ProjectVo projectVo, UserInfo loginUser) {
        List<BaseProject> list = projectMapper.duplicateChecking(projectVo.getBaseProject());
        if (list != null && list.size() != 0) {
            throw new RuntimeException("项目编号或项目名称重复");
        }

        //查询当前设计人 是否存在
        String designer = projectVo.getDesignInfo().getDesigner();
        Example example = new Example(MemberManage.class);
        //去空格
        example.createCriteria().andEqualTo("memberName", designer.trim());
        List<MemberManage> memberManages = memberManageDao.selectByExample(example);
        if (memberManages.size() > 0) {
            //如果出现重名 则只取第一个人
            MemberManage memberManagesFirst = memberManages.get(0);
            if (memberManagesFirst != null) {
                projectVo.getDesignInfo().setDesigner(memberManagesFirst.getId());
            }
        } else {
            throw new RuntimeException("设计人不存在,请重新填写");
        }

        //baseProject, designInfo, packageCame, projectExploration
        String projectuuid = UUID.randomUUID().toString().replaceAll("-", "");
        String DesignInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
        String packageCameuuId = UUID.randomUUID().toString().replaceAll("-", "");
        String projectExplorationuuid = UUID.randomUUID().toString().replaceAll("-", "");
        //时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createTime = simpleDateFormat.format(new Date());


        projectVo.getBaseProject().setId(projectuuid);
        projectVo.getBaseProject().setCreateTime(createTime);
        //todo loginUser.getId()
        projectVo.getBaseProject().setFounderId(loginUser.getId());
        projectVo.getBaseProject().setFounderCompanyId(loginUser.getCompanyId());
        projectVo.getBaseProject().setProjectFlow("1");
        projectVo.getBaseProject().setDelFlag("0");

        //提交后状态为未审核  添加后状态为出图中 如果没有互审人 说明时保存
        if (projectVo.getBaseProject().getReviewerId() == null || "".equals(projectVo.getBaseProject().getReviewerId())) {
            projectVo.getBaseProject().setDesginStatus("2");
        } else {
            AuditInfo auditInfo = new AuditInfo();
            String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
            auditInfo.setId(auditInfouuid);
            auditInfo.setBaseProjectId(DesignInfouuid);
            auditInfo.setAuditType("0");
            auditInfo.setAuditResult("0");
            auditInfo.setAuditorId(projectVo.getBaseProject().getReviewerId());
            auditInfo.setCreateTime(createTime);
            //todo   loginUser.getId()
            auditInfo.setFounderId(loginUser.getId());
            auditInfo.setCompanyId(loginUser.getCompanyId());
            auditInfo.setStatus("0");
            auditInfo.setChangeFlag("1");
            auditInfoDao.insert(auditInfo);
            projectVo.getBaseProject().setDesginStatus("1");

                MessageVo messageVo = new MessageVo();
                String id = projectVo.getBaseProject().getReviewerId();
                MemberManage memberManage = memberManageDao.selectByPrimaryKey(id);
                //审核人名字
                String name = memberManage.getMemberName();
                messageVo.setId("A03");
                messageVo.setType("1"); // 1通知
                messageVo.setUserId(loginUser.getId());
                messageVo.setTitle("您有一个设计项目待审批！");
                messageVo.setDetails(name + "您好！【" + loginUser.getUsername() + "】已将【所选项目名称】的设计项目提交给您，请审批！");
                //调用消息Service
                messageService.sendOrClose(messageVo);
        }

        projectMapper.insert(projectVo.getBaseProject());

        //设计表添加
        projectVo.getDesignInfo().setId(DesignInfouuid);
        projectVo.getDesignInfo().setBaseProjectId(projectuuid);
        //todo   loginUser.getId()
        projectVo.getDesignInfo().setFounderId(loginUser.getId());
        projectVo.getDesignInfo().setCompanyId(loginUser.getCompanyId());
        projectVo.getDesignInfo().setStatus("0");
        projectVo.getDesignInfo().setIsdeschange("0");
        projectVo.getDesignInfo().setCreateTime(createTime);
        designInfoMapper.insert(projectVo.getDesignInfo());


        //方案会审
        if (projectVo.getPackageCame().getParticipant() != null && !"".equals(projectVo.getPackageCame().getParticipant())) {
            projectVo.getPackageCame().setId(packageCameuuId);
            projectVo.getPackageCame().setBassProjectId(DesignInfouuid);
            //todo   loginUser.getId()
            projectVo.getPackageCame().setFounderId(loginUser.getId());
            projectVo.getPackageCame().setCompanyId(loginUser.getCompanyId());
            projectVo.getPackageCame().setStatus("0");
            projectVo.getPackageCame().setCreateTime(createTime);
            packageCameMapper.insert(projectVo.getPackageCame());
        }
//        else{
//            projectVo.getPackageCame().setId(packageCameuuId);
//            projectVo.getPackageCame().setBassProjectId(DesignInfouuid);
//            projectVo.getPackageCame().setStatus("0");
//            projectVo.getPackageCame().setCreateTime(createTime);
//            packageCameMapper.insert(projectVo.getPackageCame());
//        }


        //项目踏勘
        if (projectVo.getProjectExploration().getScout() != null && !"".equals(projectVo.getPackageCame().getParticipant())) {
            projectVo.getProjectExploration().setId(projectExplorationuuid);
            projectVo.getProjectExploration().setBaseProjectId(DesignInfouuid);
            //todo   loginUser.getId()
            projectVo.getProjectExploration().setFounderId(loginUser.getId());
            projectVo.getProjectExploration().setCompany_id(loginUser.getCompanyId());
            projectVo.getProjectExploration().setStatus("0");
            projectExplorationMapper.insert(projectVo.getProjectExploration());
        } else {
            projectVo.getProjectExploration().setId(projectExplorationuuid);
            projectVo.getProjectExploration().setBaseProjectId(DesignInfouuid);
            projectVo.getProjectExploration().setStatus("0");
            projectVo.getProjectExploration().setCreateTime(createTime);
            projectExplorationMapper.insert(projectVo.getProjectExploration());
        }

        //上传文件 项目踏勘文件
        List<FileInfo> byFreignAndType1 = fileInfoMapper.findByFreignAndType(projectVo.getKey(), projectVo.getType1());
        for (FileInfo fileInfo : byFreignAndType1) {
            fileInfo.setPlatCode(DesignInfouuid);
            fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
        }
        //上传文件 方案会审
        List<FileInfo> byFreignAndType2 = fileInfoMapper.findByFreignAndType(projectVo.getKey(), projectVo.getType2());
        for (FileInfo fileInfo : byFreignAndType2) {
            fileInfo.setPlatCode(DesignInfouuid);
            fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
        }
        //上传文件 客户提供图纸 批文资料
        List<FileInfo> byFreignAndType3 = fileInfoMapper.findByFreignAndType(projectVo.getKey(), projectVo.getType3());
        for (FileInfo fileInfo : byFreignAndType3) {
            fileInfo.setPlatCode(DesignInfouuid);
            fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
        }
        //上传文件 设计信息
        List<FileInfo> byFreignAndType4 = fileInfoMapper.findByFreignAndType(projectVo.getKey(), projectVo.getType4());
        for (FileInfo fileInfo : byFreignAndType4) {
            fileInfo.setPlatCode(DesignInfouuid);
            fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
        }

    }

    public BaseProject BaseProjectByid(String id) {
        BaseProject baseProject = projectMapper.selectById(id);
        return baseProject;
    }

    public BaseProject BaseProjectByid2(String id) {
        BaseProject baseProject = projectMapper.selectById2(id);
        return baseProject;
    }

    public BaseProject baseProjectByPrimaryKey(String id) {
        BaseProject baseProject = projectMapper.selectByPrimaryKey(id);
        return baseProject;
    }

    public AnhuiMoneyinfo anhuiMoneyinfoByid(String id) {
        Example example = new Example(AnhuiMoneyinfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(example);
        return anhuiMoneyinfo;
    }

    public WujiangMoneyInfo wujiangMoneyInfoByid(String id) {
        Example example = new Example(WujiangMoneyInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(example);
        return wujiangMoneyInfo;
    }

    public ProjectExploration ProjectExplorationByid(String id) {
        Example example = new Example(ProjectExploration.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        ProjectExploration projectExploration = projectExplorationMapper.selectOneByExample(example);
        return projectExploration;
    }

    public PackageCame PackageCameByid(String id) {
        Example example = new Example(PackageCame.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("bassProjectId", id);
        PackageCame packageCame = packageCameMapper.selectOneByExample(example);
        return packageCame;
    }

    /**
     * 根据基本信息id查询设计表信息
     *
     * @param id
     * @return
     */
    public DesignInfo designInfoByid(String id) {
        Example example = new Example(DesignInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        DesignInfo designInfo = designInfoMapper.selectOneByExample(example);
        return designInfo;
    }

    /**
     * 根据设计id查询
     *
     * @param id
     * @return
     */
    public DesignInfo designInfoByPrimaryKey(String id) {
        return designInfoMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询设计变更次数
     *
     * @param id
     * @return
     */
    public List<DesignChangeInfo> designChangeInfosByid(String id) {
        Example example = new Example(DesignChangeInfo.class);
        example.setOrderByClause("design_change_time desc");
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("designInfoId", id);
        List<DesignChangeInfo> designChangeInfos = designChangeInfoMapper.selectByExample(example);

        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(id);


        return designChangeInfos;
    }

    public DesignChangeInfo designChangeInfoByid(String id) {
        List<DesignChangeInfo> designChangeInfos = this.designChangeInfosByid(id);
        if (designChangeInfos.size() > 0) {
            DesignChangeInfo designChangeInfo = designChangeInfos.get(0);
            return designChangeInfo;
        }
        return null;
    }

    public List<AuditInfo> auditInfoList(String id) {
        Example example = new Example(AuditInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        c.andNotEqualTo("auditResult", "0");
        List<AuditInfo> auditInfos = auditInfoDao.selectByExample(example);
        return auditInfos;
    }

    /**
     * 项目提交-编辑-保存
     *
     * @param projectVo
     */
    public void projectEdit(ProjectVo projectVo, UserInfo loginUser) {
        List<BaseProject> list = projectMapper.duplicateCheckingByUpdate(projectVo.getBaseProject());
        if (list != null && list.size() != 0) {
            throw new RuntimeException("项目编号或项目名称重复");
        }
        //查询当前设计人 是否存在
        String designer = projectVo.getDesignInfo().getDesigner();
        Example designerExample = new Example(MemberManage.class);
        //去空格
        designerExample.createCriteria().andEqualTo("memberName", designer.trim());
        List<MemberManage> memberManages = memberManageDao.selectByExample(designerExample);
        if (memberManages.size() > 0) {
            //如果出现重名 则只取第一个人
            MemberManage designerFirst = memberManages.get(0);
            if (designerFirst != null) {
                projectVo.getDesignInfo().setDesigner(designerFirst.getId());
            }
        } else {
            throw new RuntimeException("设计人不存在,请重新填写");
        }

        //方便测试
        //todo loginUser.getId(); loginUser.getCompanyId();
        String loginUserId = loginUser.getId();
//        String loginUserId = "user309";
        String companyId = loginUser.getCompanyId();
//        String companyId = "com";
        String username = loginUser.getUsername();
//        String username = "集团领导";

        //BaseProject baseProject, DesignInfo designInfo, ProjectExploration projectExploration, PackageCame packageCame
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String updateTime = simpleDateFormat.format(new Date());
        //如果按钮状态为1 说明点击的是提交
        if ("1".equals(projectVo.getBaseProject().getOrsubmit())) {
            //如果提交人为空 说明时保存状态未出图中 反之状态未通过
            if (projectVo.getBaseProject().getReviewerId() == null || "".equals(projectVo.getBaseProject().getReviewerId())) {
                if ("3".equals(projectVo.getBaseProject().getDesginStatus())) {
                    //如果是未通过
                    Example example = new Example(AuditInfo.class);
                    Example.Criteria criteria = example.createCriteria();
                    criteria.andEqualTo("baseProjectId", projectVo.getDesignInfo().getId());
                    criteria.andEqualTo("auditResult", "2");
                    AuditInfo auditInfo1 = auditInfoDao.selectOneByExample(example);
                    //如果是未通过 提交时 将审核信息改为待审核
                    auditInfo1.setAuditResult("0");
                    auditInfo1.setAuditOpinion("");
                    auditInfo1.setAuditTime("");
                    //修改基本状态 未通过重新变为待审核
                    projectVo.getBaseProject().setReviewerId(auditInfo1.getAuditorId());
                    projectVo.getBaseProject().setDesginStatus("1");
                    auditInfoDao.updateByPrimaryKeySelective(auditInfo1);

                    //消息通知 未通过提醒
                    MessageVo messageVo = new MessageVo();
                    String projectName = projectVo.getBaseProject().getProjectName();

                    MemberManage memberManage = memberManageDao.selectByPrimaryKey(auditInfo1.getAuditorId());
                    //审核人名字
                    String name = memberManage.getMemberName();
                    messageVo.setId("A04");
                    messageVo.setUserId(loginUserId);
                    messageVo.setType("1"); // 1 通知
                    messageVo.setTitle("您有一个设计项目未通过！");
                    messageVo.setDetails(name + "您好！【" + username + "】已将【" + projectName + "】的设计项目未通过审核，请及时查看详情！");
                    //调用消息Service
                    messageService.sendOrClose(messageVo);
                }
            } else {
                if ("2".equals(projectVo.getBaseProject().getDesginStatus())) {
                    //如果是出图中 则需要选择互审人
                    AuditInfo auditInfo = new AuditInfo();
                    String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
                    auditInfo.setId(auditInfouuid); //id
                    auditInfo.setBaseProjectId(projectVo.getDesignInfo().getId()); //外键
                    auditInfo.setAuditType("0"); //状态为一审
                    auditInfo.setAuditResult("0"); //待审核
                    auditInfo.setChangeFlag("1");
                    auditInfo.setAuditorId(projectVo.getBaseProject().getReviewerId()); //审核人
                    auditInfo.setCreateTime(updateTime); //创建时间
                    auditInfo.setFounderId(loginUserId); //创建人
                    auditInfo.setCompanyId(companyId); //创建人公司
                    auditInfo.setStatus("0"); //状态
                    //将互审人信息写入审核表
                    auditInfoDao.insert(auditInfo);
                    //审核状态从出图中变为待审核
                    projectVo.getBaseProject().setDesginStatus("1");
                    //消息通知 未通过提醒
                    MessageVo messageVo = new MessageVo();
                    String projectName = projectVo.getBaseProject().getProjectName();
                    MemberManage memberManage = memberManageDao.selectByPrimaryKey(auditInfo.getAuditorId());
                    //审核人名字
                    String name = memberManage.getMemberName();
                    messageVo.setId("A04");
                    messageVo.setUserId(loginUserId);
                    messageVo.setType("1"); // 1 通知
                    messageVo.setTitle("您有一个设计项目未通过！");
                    messageVo.setDetails(name + "您好！【" + username + "】已将【" + projectName + "】的设计项目未通过审核，请及时查看详情！");
                    //调用消息Service
                    messageService.sendOrClose(messageVo);
                }
            }
        } else {
            //如果不为1 则为保存 状态依旧是出图中
            projectVo.getBaseProject().setDesginStatus("2");
        }

        //添加修改时间
        projectVo.getBaseProject().setUpdateTime(updateTime);
        projectMapper.updateByPrimaryKeySelective(projectVo.getBaseProject());
        //添加设计表修改时间
        projectVo.getDesignInfo().setUpdateTime(updateTime);
        projectVo.getDesignInfo().setIsdeschange("0");
        designInfoMapper.updateByPrimaryKeySelective(projectVo.getDesignInfo());
        //添加勘探表时间
        if (projectVo.getProjectExploration() != null) {
            projectVo.getProjectExploration().setUpdateTime(updateTime);
            ProjectExploration projectExploration = this.ProjectExplorationByid(projectVo.getBaseProject().getId());
            if (projectExploration != null) {
                projectVo.getProjectExploration().setId(projectExploration.getId());
                projectExplorationMapper.updateByPrimaryKeySelective(projectVo.getProjectExploration());
            }
        }
        //方案会审
        if (projectVo.getProjectExploration() != null) {
            projectVo.getPackageCame().setUpdateTime(updateTime);
            PackageCame packageCame = this.PackageCameByid(projectVo.getBaseProject().getId());
            if (packageCame != null) {
                projectVo.getPackageCame().setId(packageCame.getId());
                packageCameMapper.updateByPrimaryKeySelective(projectVo.getPackageCame());
            }
        }


//        if("3".equals(projectVo.getBaseProject().getDesginStatus())){
//            //如果按钮状态为1 说明点击的是提交
//            if("1".equals(projectVo.getBaseProject().getOrsubmit())){
//                //审核状态从未通过中变为待审核
//                projectVo.getBaseProject().setDesginStatus("1");
//            }else {
//                //如果不为1 则为保存 状态依旧是未通过
//                projectVo.getBaseProject().setDesginStatus("3");
//            }
//                //添加修改时间
//            projectVo.getBaseProject().setUpdateTime(updateTime);
//            projectMapper.updateByPrimaryKeySelective(projectVo.getBaseProject());
//            //添加设计表修改时间
//            projectVo.getDesignInfo().setUpdateTime(updateTime);
//            designInfoMapper.updateByPrimaryKeySelective(projectVo.getDesignInfo());
//            //添加勘探表时间
//            if(projectVo.getProjectExploration()!=null){
//                projectVo.getProjectExploration().setUpdateTime(updateTime);
//                ProjectExploration projectExploration = this.ProjectExplorationByid(projectVo.getBaseProject().getId());
//                if(projectExploration!=null){
//                    projectVo.getProjectExploration().setId(projectExploration.getId());
//                    projectExplorationMapper.updateByPrimaryKeySelective(projectVo.getProjectExploration());
//                }
//            }
//            //方案会审
//            if(projectVo.getProjectExploration()!=null){
//                projectVo.getPackageCame().setUpdateTime(updateTime);
//                PackageCame packageCame = this.PackageCameByid(projectVo.getBaseProject().getId());
//                if(packageCame!=null){
//                    projectVo.getPackageCame().setId(packageCame.getId());
//                    packageCameMapper.updateByPrimaryKeySelective(projectVo.getPackageCame());
//                }
//            }
//        }

    }

    @Resource
    private ProjectSumService projectSumService;

    /**
     * 安徽信息回显
     *
     * @param id
     * @return
     */
    public AnhuiMoneyinfo anhuiMoneyInfoSelect(String id) {
        Example example = new Example(AnhuiMoneyinfo.class);
        example.createCriteria().andEqualTo("baseProjectId", id);
        AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(example);
        if (anhuiMoneyinfo != null) {
            anhuiMoneyinfo.setSelectFlag("1");
            return anhuiMoneyinfo;
        } else {
            return new AnhuiMoneyinfo();
        }
    }

    /**
     * 吴江信息回显
     *
     * @param id
     * @return
     */
    public WujiangMoneyInfo wujiangMoneyInfoSelect(String id) {
        Example example = new Example(WujiangMoneyInfo.class);
        example.createCriteria().andEqualTo("baseProjectId", id);
        WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(example);
        if (wujiangMoneyInfo != null) {
            wujiangMoneyInfo.setSelectFlag("1");
            return wujiangMoneyInfo;
        } else {
            return new WujiangMoneyInfo();
        }
    }

    /**
     * 安徽代收列表
     *
     * @return
     */
    public List<CollectionMoney> anhuiCollectionMoney(String id) {
        ArrayList<CollectionMoney> collectionMonies = new ArrayList<>();
        Example example = new Example(AnhuiMoneyinfo.class);
        example.createCriteria().andEqualTo("baseProjectId", id);
        AnhuiMoneyinfo anhuiMoneyinfo = anhuiMoneyinfoMapper.selectOneByExample(example);
        if (anhuiMoneyinfo != null) {
            String[] split = anhuiMoneyinfo.getCollectionMoney().split(",");
            Integer count = 1;
            CollectionMoney collectionMoney = new CollectionMoney();
            for (String money : split) {
                collectionMoney.setId("第" + (count) + "次收款");
                collectionMoney.setMoney(money);
                collectionMonies.add(collectionMoney);
                count++;
            }
        }
        return collectionMonies;
    }

    /**
     * 吴江代收列表
     *
     * @param id
     * @return
     */
    public List<CollectionMoney> wujiangCollectionMoney(String id) {
        ArrayList<CollectionMoney> collectionMonies = new ArrayList<>();
        Example example = new Example(WujiangMoneyInfo.class);
        example.createCriteria().andEqualTo("baseProjectId", id);
        WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfoMapper.selectOneByExample(example);
        if (wujiangMoneyInfo != null) {
            String[] split = wujiangMoneyInfo.getCollectionMoney().split(",");
            Integer count = 1;
            CollectionMoney collectionMoney = new CollectionMoney();
            for (String money : split) {
                collectionMoney.setId("第" + (count) + "次收款");
                collectionMoney.setMoney(money);
                collectionMonies.add(collectionMoney);
                count++;
            }
        }
        return collectionMonies;
    }


    /**
     * 添加安徽信息
     *
     * @param anhuiMoneyinfo
     */
    public void anhuiMoneyInfoAdd(AnhuiMoneyinfo anhuiMoneyinfo, UserInfo loginUser) throws Exception {
        //实收金额
        BigDecimal officialReceipts = anhuiMoneyinfo.getOfficialReceipts();
        if(!"".equals(officialReceipts) && officialReceipts != null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            Example example = new Example(AnhuiMoneyinfo.class);
            Example.Criteria c = example.createCriteria();
//        IncomeInfo incomeInfo = new IncomeInfo();

            anhuiMoneyinfo.setId(uuid);
//        anhuiMoneyinfo.setFounderId(loginUser.getId());
//        anhuiMoneyinfo.setCompanyId(loginUser.getCompanyId());
            anhuiMoneyinfo.setStatus("0");
            anhuiMoneyinfo.setCreateTime(simpleDateFormat.format(new Date()));

            //根据设计表id 查询数据取出代收金额
            c.andEqualTo("baseProjectId", anhuiMoneyinfo.getBaseProjectId());
            if ("1".equals(anhuiMoneyinfo.getPayTerm())) {
                //获取应收金额
                AnhuiMoneyinfo anhuiMoneyinfo1 = anhuiMoneyinfoMapper.selectOneByExample(example);
                //获取代收金额信息
                //如果代收金额为空 说明第一次代收
                if (anhuiMoneyinfo1 == null) {
                    //将代收信息拼接 保存到对象中
                    String newcollectionMoney =  officialReceipts + ",";
                    anhuiMoneyinfo.setCollectionMoney(newcollectionMoney);
                    String newCollectIonMoney = newcollectionMoney.substring(0,newcollectionMoney.length()-1);
                    anhuiMoneyinfo.setTotalMoney(new BigDecimal(newCollectIonMoney));

                    //如果代收金额超过或者等于 应收金额后
                    if (anhuiMoneyinfo.getRevenue().compareTo(anhuiMoneyinfo.getTotalMoney()) <= 0) {
                        //同时返回标识 改账单已支付完成
                        designInfoMapper.updateFinalAccount(anhuiMoneyinfo.getBaseProjectId());
                    }
                    anhuiMoneyinfoMapper.insert(anhuiMoneyinfo);
                } else {
                    String collectionMoney = anhuiMoneyinfo1.getCollectionMoney();
                    //若果不是说明第一次添加
                    anhuiMoneyinfo.setCollectionMoney(officialReceipts+collectionMoney + ",");
                    String[] split = collectionMoney.split(",");
                    Double total = 0.0;
                    for (String s : split) {
                        total += Double.parseDouble(s);
                    }
                    //保存收入表信息
//                incomeInfo.setBaseProjectId(anhuiMoneyinfo.getBaseProjectId());
                    anhuiMoneyinfo.setTotalMoney(new BigDecimal(total));
//                incomeInfo.setDesignMoney(new BigDecimal(collectionMoney));
//                projectSumService.addIncomeInfo(incomeInfo);
                    anhuiMoneyinfoMapper.updateByPrimaryKeySelective(anhuiMoneyinfo);
                }
            } else {
                //如果是实收 则直接添加到表中
                anhuiMoneyinfoMapper.insert(anhuiMoneyinfo);
                //同时返回标识 改账单已支付完成
                designInfoMapper.updateFinalAccount(anhuiMoneyinfo.getBaseProjectId());

//            //同时将设计费添加到总收入表中
//            incomeInfo.setBaseProjectId(anhuiMoneyinfo.getBaseProjectId());
//            incomeInfo.setFounderCompanyId(loginUser.getCompanyId());
//            incomeInfo.setFounderId(loginUser.getId());
//            incomeInfo.setDelFlag("0");
//            incomeInfo.setDesignMoney(anhuiMoneyinfo.getOfficialReceipts());
//            projectSumService.addIncomeInfo(incomeInfo);
            }
            //根据安徽到账外键查找设计
            DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(anhuiMoneyinfo.getBaseProjectId());
            BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());

//        DesignInfo designInfo = designInfoByid(baseProject.getId());
            // 计算应计提金额、实际计提金额、余额
            //设计费（安徽）
            AnhuiMoneyinfo anhuiMoneyinfo1 = anhuiMoneyInfopayterm(designInfo.getId());
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (anhuiMoneyinfo1 != null) {
                baseProject.setDesMoney(anhuiMoneyinfo1.getOfficialReceipts());
                //应计提金额
                BigDecimal bigDecimal = accruedAmount(anhuiMoneyinfo1.getOfficialReceipts());
                //实际计提金额
                BigDecimal bigDecimal1 = proposedAmount(bigDecimal);
                // 余额
                BigDecimal surplus = surplus(bigDecimal, bigDecimal1);
                //设计绩效统计
                EmployeeAchievementsInfo achievementsInfo = new EmployeeAchievementsInfo();
                achievementsInfo.setId(UUID.randomUUID().toString().replaceAll("-",""));
                achievementsInfo.setMemberId(designInfo.getDesigner()); //当前设计人
                achievementsInfo.setCreateTime(data);
                achievementsInfo.setUpdateTime(data);

                achievementsInfo.setFounderId(designInfo.getFounderId()); //当前创建人
                achievementsInfo.setFounderCompanyId(designInfo.getCompanyId()); //当前创建人公司
                achievementsInfo.setDelFlag("0");
                achievementsInfo.setDistrict(baseProject.getDistrict()); //所属地区
                achievementsInfo.setDept("1"); //设计部门
                achievementsInfo.setAchievementsType("1"); // 设计绩效
                achievementsInfo.setAccruedAmount(bigDecimal); //应计提金额
                achievementsInfo.setActualAmount(bigDecimal1); //实际计提金额
                achievementsInfo.setBalance(surplus); //余额
                achievementsInfo.setBaseProjectId(baseProject.getId()); //绩效外键
                achievementsInfo.setProjectNum(designInfo.getId());
                achievementsInfo.setOverFlag("0"); // 绩效是否发放完成 默认未完成
                //存入数据库
                employeeAchievementsInfoMapper.insertSelective(achievementsInfo);
            }
//        else {
//            //设计费（吴江）
//            WujiangMoneyInfo wujiangMoneyInfo = wujiangMoneyInfopayterm(designInfo.getId());
//            if (wujiangMoneyInfo != null) {
//                baseProject.setDesMoney(wujiangMoneyInfo.getOfficialReceipts());
//                //应计提金额
//                BigDecimal bigDecimal = accruedAmount(wujiangMoneyInfo.getOfficialReceipts());
//                baseProject.setAccrualMoney(bigDecimal.doubleValue());
//                //建议金额
//                BigDecimal bigDecimal1 = proposedAmount(bigDecimal);
//                baseProject.setAdviseMoney(bigDecimal1.doubleValue());
//                // 余额
//                BigDecimal surplus = surplus(bigDecimal, bigDecimal1);
//                baseProject.setSurplus(surplus.doubleValue());
//            }
//        }
        }else{
            throw new Exception("请输入实收金额！！！");
        }

    }

    /**
        * @Author sjf
        * @Description //添加吴江信息（新）
        * @Date 19:48 2020/11/27
        * @Param
        * @return
     **/
    public void wujiangMoneyInfoAdd(WujiangMoneyInfo wujiangMoneyInfo, UserInfo loginUser) throws Exception {
        //实收金额
        BigDecimal officialReceipts = wujiangMoneyInfo.getOfficialReceipts();
        String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        if(!"".equals(officialReceipts) && officialReceipts != null){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            Example example = new Example(WujiangMoneyInfo.class);
            Example.Criteria c = example.createCriteria();

            wujiangMoneyInfo.setId(uuid);
            wujiangMoneyInfo.setStatus("0");
            wujiangMoneyInfo.setCreateTime(data);

            //根据设计表id 查询数据取出代收金额
            c.andEqualTo("baseProjectId", wujiangMoneyInfo.getBaseProjectId());
            if ("1".equals(wujiangMoneyInfo.getPayTerm())) {
                //获取应收金额
                WujiangMoneyInfo wujiangMoneyInfo1 = wujiangMoneyInfoMapper.selectOneByExample(example);
                //获取代收金额信息
                //如果代收金额为空 说明第一次代收
                if (wujiangMoneyInfo1 == null) {
                    //将代收信息拼接 保存到对象中
                    String newcollectionMoney =  officialReceipts + ",";
                    wujiangMoneyInfo.setCollectionMoney(newcollectionMoney);
                    String newCollectIonMoney = newcollectionMoney.substring(0,newcollectionMoney.length()-1);
                    wujiangMoneyInfo.setTotalMoney(new BigDecimal(newCollectIonMoney));

                    //如果代收金额超过或者等于 应收金额后
                    if (wujiangMoneyInfo.getRevenue().compareTo(wujiangMoneyInfo.getTotalMoney()) <= 0) {
                        //同时返回标识 改账单已支付完成
                        designInfoMapper.updateFinalAccount(wujiangMoneyInfo.getBaseProjectId());
                    }
                    wujiangMoneyInfoMapper.insert(wujiangMoneyInfo);
                } else {
                    String collectionMoney = wujiangMoneyInfo1.getCollectionMoney();
                    //若果不是说明第一次添加
                    wujiangMoneyInfo.setCollectionMoney(officialReceipts+collectionMoney + ",");
                    String[] split = collectionMoney.split(",");
                    Double total = 0.0;
                    for (String s : split) {
                        total += Double.parseDouble(s);
                    }
                    wujiangMoneyInfo.setTotalMoney(new BigDecimal(total));

                    wujiangMoneyInfoMapper.updateByPrimaryKeySelective(wujiangMoneyInfo);
                }
            } else {
                //如果是实收 则直接添加到表中
                wujiangMoneyInfoMapper.insert(wujiangMoneyInfo);
                //同时返回标识 改账单已支付完成
                designInfoMapper.updateFinalAccount(wujiangMoneyInfo.getBaseProjectId());

            }
            //根据安徽到账外键查找设计
            DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(wujiangMoneyInfo.getBaseProjectId());
            BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());

            // 计算应计提金额、实际计提金额、余额
            //设计费（吴江）
            WujiangMoneyInfo wujiangMoneyInfo1 = wujiangMoneyInfopayterm(designInfo.getId());
            if (wujiangMoneyInfo1 != null) {
                baseProject.setDesMoney(wujiangMoneyInfo1.getOfficialReceipts());
                //应计提金额
                BigDecimal bigDecimal = accruedAmount(wujiangMoneyInfo1.getOfficialReceipts());
                //实际计提金额
                BigDecimal bigDecimal1 = proposedAmount(bigDecimal);
                // 余额
                BigDecimal surplus = surplus(bigDecimal, bigDecimal1);
                //设计绩效统计
                EmployeeAchievementsInfo achievementsInfo = new EmployeeAchievementsInfo();
                achievementsInfo.setId(UUID.randomUUID().toString().replaceAll("-",""));
                achievementsInfo.setMemberId(designInfo.getDesigner()); //当前设计人
                achievementsInfo.setCreateTime(data);
                achievementsInfo.setUpdateTime(data);
                achievementsInfo.setFounderId(designInfo.getFounderId()); //当前创建人
                achievementsInfo.setFounderCompanyId(designInfo.getCompanyId()); //当前创建人公司
                achievementsInfo.setDelFlag("0");
                achievementsInfo.setDistrict(baseProject.getDistrict()); //所属地区
                achievementsInfo.setDept("1"); //设计部门
                achievementsInfo.setAchievementsType("1"); // 设计绩效
                achievementsInfo.setAccruedAmount(bigDecimal); //应计提金额
                achievementsInfo.setActualAmount(bigDecimal1); //实际计提金额
                achievementsInfo.setBalance(surplus); //余额
                achievementsInfo.setBaseProjectId(baseProject.getId()); //绩效外键
                achievementsInfo.setProjectNum(designInfo.getId());
                achievementsInfo.setOverFlag("0"); // 绩效是否发放完成 默认未完成
                //存入数据库
                employeeAchievementsInfoMapper.insertSelective(achievementsInfo);
            }
        }else{
            throw new Exception("请输入实收金额！！！");
        }


    }

    /**
     * 添加吴江信息
     *
     * @param wujiangMoneyInfo
     */
    /*public void wujiangMoneyInfoAdd(WujiangMoneyInfo wujiangMoneyInfo, UserInfo loginUser) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        Example example = new Example(WujiangMoneyInfo.class);
        Example.Criteria c = example.createCriteria();
        //根据设计表id 查询数据取出代收金额
        c.andEqualTo("baseProjectId", wujiangMoneyInfo.getBaseProjectId());

        IncomeInfo incomeInfo = new IncomeInfo();

        wujiangMoneyInfo.setId(uuid);
        wujiangMoneyInfo.setFounderId(loginUser.getId());
        wujiangMoneyInfo.setCompanyId(loginUser.getCompanyId());
        wujiangMoneyInfo.setStatus("0");
        wujiangMoneyInfo.setCreateTime(simpleDateFormat.format(new Date()));

        if ("1".equals(wujiangMoneyInfo.getPayTerm())) {
            //获取应收金额
            BigDecimal officialReceipts = wujiangMoneyInfo.getOfficialReceipts();
            WujiangMoneyInfo wujiangMoneyInfo1 = wujiangMoneyInfoMapper.selectOneByExample(example);
            //获取代收金额信息
            String collectionMoney = wujiangMoneyInfo1.getCollectionMoney();
            //如果代收金额为空 说明第一次代收
            if (wujiangMoneyInfo1.getCollectionMoney() != null || !"".equals(wujiangMoneyInfo1.getCollectionMoney())) {
                //将代收信息拼接 保存到对象中
                String newcollectionMoney = collectionMoney + officialReceipts + ",";
                wujiangMoneyInfo.setCollectionMoney(newcollectionMoney);
                String[] split = collectionMoney.split(",");
                Double total = 0.0;
                for (String s : split) {
                    total += Double.parseDouble(s);
                }
                //如果代收金额超过或者等于 应收金额后
                if (wujiangMoneyInfo.getRevenue().compareTo(new BigDecimal(total)) < 1) {
                    designInfoMapper.updateFinalAccount(wujiangMoneyInfo.getBaseProjectId());
                }
            } else {
                wujiangMoneyInfo.setCollectionMoney(collectionMoney + ",");

                //保存收入表信息
                incomeInfo.setBaseProjectId(wujiangMoneyInfo.getBaseProjectId());
                incomeInfo.setDesignMoney(new BigDecimal(collectionMoney));
                projectSumService.addIncomeInfo(incomeInfo);
            }
            wujiangMoneyInfoMapper.updateByPrimaryKeySelective(wujiangMoneyInfo);
        } else {
            wujiangMoneyInfoMapper.insert(wujiangMoneyInfo);
            designInfoMapper.updateFinalAccount(wujiangMoneyInfo.getBaseProjectId());

            //同时将设计费添加到总收入表中
            incomeInfo.setBaseProjectId(wujiangMoneyInfo.getBaseProjectId());
            incomeInfo.setFounderCompanyId(loginUser.getCompanyId());
            incomeInfo.setFounderId(loginUser.getId());
            incomeInfo.setDelFlag("0");
            incomeInfo.setDesignMoney(wujiangMoneyInfo.getOfficialReceipts());
            projectSumService.addIncomeInfo(incomeInfo);
        }
    }*/

    /**
     * 吴江到账信息回显
     *
     * @param id
     * @return
     */
    public WujiangMoneyInfo wujiangMoneyInfopayterm(String id) {
        Example example = new Example(WujiangMoneyInfo.class);
        Example.Criteria c = example.createCriteria();
        //根据设计表id 查询数据取出代收金额
        c.andEqualTo("baseProjectId", id);
        WujiangMoneyInfo wujiangMoneyInfo1 = wujiangMoneyInfoMapper.selectOneByExample(example);
        if (wujiangMoneyInfo1 != null) {
            Double total = 0.0;
            //获取代收金额记录
            String collectionMoney = wujiangMoneyInfo1.getCollectionMoney();
            if(collectionMoney!=null){
                String[] split = collectionMoney.split(",");
                ArrayList<PayItem> strings = new ArrayList<>();
                for (int i = 0; i < split.length; i++) {
                    PayItem payItem = new PayItem();
                    payItem.setNum("第" + (i + 1) + "次收款");
                    payItem.setSize(split[i] + "元");
                    strings.add(payItem);
                    total += Double.parseDouble(split[i]);
                }
                //将数组返回
                wujiangMoneyInfo1.setStrings(strings);
                //将总金额返回
                wujiangMoneyInfo1.setTotalMoney(new BigDecimal(total));
                return wujiangMoneyInfo1;
            }
        }
        return null;
    }

    /**
     * 安徽到账信息回显
     *
     * @param id
     * @return
     */
    public AnhuiMoneyinfo anhuiMoneyInfopayterm(String id) {
        Example example = new Example(AnhuiMoneyinfo.class);
        Example.Criteria c = example.createCriteria();
        //根据设计表id 查询数据取出代收金额
        c.andEqualTo("baseProjectId",id);
        AnhuiMoneyinfo anhuiMoneyinfo1 = anhuiMoneyinfoMapper.selectOneByExample(example);
        if(anhuiMoneyinfo1!=null){
            Double total = 0.0;
            //获取代收金额记录
            String collectionMoney = anhuiMoneyinfo1.getCollectionMoney();
            if(collectionMoney!=null){
                String[] split = collectionMoney.split(",");
                ArrayList<PayItem> strings = new ArrayList<>();
                for (int i = 0; i < split.length; i++) {
                    PayItem payItem = new PayItem();
                    payItem.setNum("第"+ (i+1) +"次收款");
                    payItem.setSize(split[i]+"元");
                    strings.add(payItem);
                    total+= Double.parseDouble(split[i]);
                }
                //将数组返回
                anhuiMoneyinfo1.setStrings(strings);
                //将总金额返回
                anhuiMoneyinfo1.setTotalMoney(new BigDecimal(total));
                return anhuiMoneyinfo1;
            }
        }
        return null;
    }

    /**
     * 设计变更编辑
     *
     * @param projectVo
     */
    public void disProjectChangeEdit(ProjectVo projectVo, UserInfo loginUser) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String updateTime = simpleDateFormat.format(new Date());
        //todo loginUser.getId(); loginUser.getCompanyId();
        String loginUserId = loginUser.getId();
        String companyId = loginUser.getCompanyId();

        Example change = new Example(DesignChangeInfo.class);
        Example.Criteria changec = change.createCriteria();
        changec.andEqualTo("designInfoId", projectVo.getDesignInfo().getId());
        changec.andEqualTo("status", "0");

        //如果按钮状态为1 说明点击的是提交
        if ("1".equals(projectVo.getBaseProject().getOrsubmit())) {
            //如果提交人为空 为空说明是未通过
            if (projectVo.getBaseProject().getReviewerId() == null || "".equals(projectVo.getBaseProject().getReviewerId())) {
                //如果是设计变更未通过
                Example example = new Example(AuditInfo.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("baseProjectId", projectVo.getDesignInfo().getId());
                criteria.andEqualTo("auditResult", "2");
                AuditInfo auditInfo1 = auditInfoDao.selectOneByExample(example);
                //如果是未通过 提交时 将审核信息改为待审核
                auditInfo1.setAuditResult("0");
                //修改基本状态 未通过重新变为待审核
                projectVo.getBaseProject().setDesginStatus("1");
                auditInfoDao.updateByPrimaryKeySelective(auditInfo1);

                //将之前的设计变更信息设为不可用
                DesignChangeInfo designChangeInfo = designChangeInfoMapper.selectOneByExample(change);
                if (designChangeInfo != null) {
                    designChangeInfo.setStatus("1"); //将该条数据删除
                    designChangeInfoMapper.updateByPrimaryKeySelective(designChangeInfo);
                }

            } else {
                //根据设计id删除之前的审核信息
                this.deleteDesChangeAudit(projectVo.getDesignInfo().getId());
                AuditInfo auditInfo = new AuditInfo();
                String auditInfouuid = UUID.randomUUID().toString().replaceAll("-", "");
                //编辑完成 写入互审人
                auditInfo.setId(auditInfouuid);
                auditInfo.setBaseProjectId(projectVo.getDesignInfo().getId());
                auditInfo.setAuditType("2");
                auditInfo.setAuditResult("0");
                auditInfo.setAuditorId(projectVo.getBaseProject().getReviewerId());
                auditInfo.setCreateTime(updateTime);
                auditInfo.setFounderId(loginUserId);
                auditInfo.setCompanyId(companyId);
                auditInfo.setStatus("0");
                auditInfo.setChangeFlag("0");
                //将互审人信息写入审核表
                auditInfoDao.insert(auditInfo);
                //审核状态从出图中变为待审核
                projectVo.getBaseProject().setDesginStatus("1");
                //将之前的设计变更信息设为不可用
                DesignChangeInfo designChangeInfo = designChangeInfoMapper.selectOneByExample(change);
                if (designChangeInfo != null) {
                    designChangeInfo.setStatus("1"); //将该条数据删除
                    designChangeInfoMapper.updateByPrimaryKeySelective(designChangeInfo);
                }
            }
        } else {
            //如果不为1 则为保存 状态依旧是出图中
            projectVo.getBaseProject().setDesginStatus("2");
        }
        //添加设计变更信息
        packageCameMapper.updateByPrimaryKeySelective(projectVo.getPackageCame());
        String DesignChangeInfoid = UUID.randomUUID().toString().replaceAll("-", "");
        projectVo.getDesignChangeInfo().setCreateTime(updateTime);
        projectVo.getDesignChangeInfo().setId(DesignChangeInfoid);
        projectVo.getDesignChangeInfo().setDesignInfoId(projectVo.getDesignInfo().getId());
        projectVo.getDesignChangeInfo().setFounderId(loginUserId);
        projectVo.getDesignChangeInfo().setCompanyId(companyId);
        projectVo.getDesignChangeInfo().setStatus("0");

        if (projectVo.getBaseProject().getReviewerId() == null || "".equals(projectVo.getBaseProject().getReviewerId())) {
            //如果为未通过将信息修改
            designChangeInfoMapper.updateByPrimaryKeySelective(projectVo.getDesignChangeInfo());
        } else {
            //添加一条设计变更信息
            designChangeInfoMapper.insert(projectVo.getDesignChangeInfo());

            //添加前 将之前的设计变更信息变为不可用
            List<FileInfo> byFreignAndTypeOld =
                    fileInfoMapper.findByFreignAndType(projectVo.getDesignInfo().getId(), projectVo.getType1());
            for (FileInfo fileInfo : byFreignAndTypeOld) {
                fileInfo.setStatus("1"); //设置为不可用
                fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
            }

            //添加设计变更文件
            List<FileInfo> byFreignAndType1 = fileInfoMapper.findByFreignAndType(projectVo.getKey(), projectVo.getType1());
            for (FileInfo fileInfo : byFreignAndType1) {
                //由于在页面取不到设计变更id 所以用设计表id
                fileInfo.setPlatCode(projectVo.getDesignInfo().getId());
                fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
            }
        }
        //同时将该条设计信息标记为设计变更信息
        Example example = new Example(DesignInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("baseProjectId", projectVo.getBaseProject().getId());
        DesignInfo designInfo = designInfoMapper.selectOneByExample(example);
        designInfo.setIsdeschange("1");
        designInfoMapper.updateByPrimaryKeySelective(designInfo);

        //添加修改时间
        projectVo.getBaseProject().setUpdateTime(updateTime);
        projectMapper.updateByPrimaryKeySelective(projectVo.getBaseProject());
    }

    /**
     * 展示当前项目的所有合并项目
     *
     * @param id 虚拟编号
     * @return
     */
    public List<BaseProject> DesProjectInfoSelect(String id) {
        Example baseProjectexample = new Example(BaseProject.class);
        Example.Criteria baseProjectc = baseProjectexample.createCriteria();
        //根据虚拟编号查询 同时不能为主表
        baseProjectc.andEqualTo("virtualCode", id);
        baseProjectc.andNotEqualTo("mergeFlag", "0");
        List<BaseProject> baseProjects = projectMapper.selectByExample(baseProjectexample);
        for (BaseProject baseProject : baseProjects) {
            //将设计表id注入
            Example example = new Example(DesignInfo.class);
            Example.Criteria Designc = example.createCriteria();
            Designc.andEqualTo("baseProjectId", baseProject.getId());
            DesignInfo designInfo = designInfoMapper.selectOneByExample(example);
            baseProject.setDesId(designInfo.getId());
        }
        return baseProjects;
    }

    /**
     * 设计审核 与 设计变更信息审核
     */
    public void DesginAudandChangeAud(AuditInfo auditInfo, UserInfo loginUser) {
        //获取当前用户
        //判断当前用户角色
        MemberManage memberManage = memberManageDao.selectByPrimaryKey(loginUser.getId());
        //根据审核id获取设计表
        DesignInfo designInfo = designInfoMapper.selectByPrimaryKey(auditInfo.getBaseProjectId());
        //通过设计表获得基础信息表
        BaseProject baseProject = projectMapper.selectByPrimaryKey(designInfo.getBaseProjectId());
        //如果虚拟编号不为空 说明是合并项目
        if (!"".equals(baseProject.getVirtualCode()) && baseProject.getVirtualCode() != null) {
            List<BaseProject> baseProjects = this.DesProjectInfoSelect(baseProject.getVirtualCode());

            for (BaseProject project : baseProjects) {
                if ("4".equals(memberManage.getMemberRoleId()) && "1".equals(memberManage.getDepAdmin())) {
                    //如果为通过 则从待审核状态变为已完成 如果为未通过则状态改为未通过
                    if ("1".equals(auditInfo.getAuditResult())) {
                        //修改当前基本信息中设计审核状态
                        project.setDesginStatus("4");
                        auditInfo.setAuditType("1");
                        projectMapper.updateByPrimaryKeySelective(project);
                    } else if ("2".equals(auditInfo.getAuditResult())) {
                        project.setDesginStatus("3");
                        auditInfo.setAuditType("0");
                        projectMapper.updateByPrimaryKeySelective(project);
                    }
                }
                if ("2".equals(auditInfo.getAuditResult())) {
                    project.setDesginStatus("3");
                    auditInfo.setAuditType("2");
                    projectMapper.updateByPrimaryKeySelective(project);
                } else if ("1".equals(auditInfo.getAuditResult())) {
                    //如果为通过则 审核状态变为一审
                    auditInfo.setAuditType("1");
                    project.setDesginStatus("1");
                    projectMapper.updateByPrimaryKeySelective(project);
                }
                DesignInfo param = this.designInfoByid(project.getId());
                Example example = new Example(AuditInfo.class);
                Example.Criteria c = example.createCriteria();
                c.andEqualTo("baseProjectId", param.getId());
                auditInfoDao.updateByExample(auditInfo, example);
            }
        } else {
            //虚拟编号为空 说明不是合并项目
            //说明他是设计部门负责人
            if ("4".equals(memberManage.getMemberRoleId()) && "1".equals(memberManage.getDepAdmin())) {
                //如果为通过 则从待审核状态变为已完成 如果为未通过则状态改为未通过
                if ("1".equals(auditInfo.getAuditResult())) {
                    baseProject.setDesginStatus("4");
                    auditInfo.setAuditType("1");
                    projectMapper.updateByPrimaryKeySelective(baseProject);
                } else if ("2".equals(auditInfo.getAuditResult())) {
                    baseProject.setDesginStatus("3");
                    projectMapper.updateByPrimaryKeySelective(baseProject);
                }
            }
            if ("2".equals(auditInfo.getAuditResult())) {
                baseProject.setDesginStatus("3");
                auditInfo.setAuditType("0");
                projectMapper.updateByPrimaryKeySelective(baseProject);
            } else if ("1".equals(auditInfo.getAuditResult())) {
                //如果为通过则 审核状态变为一审
                auditInfo.setAuditType("1");
            }
            Example example = new Example(AuditInfo.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("baseProjectId", designInfo.getId());
            auditInfoDao.updateByExample(auditInfo, example);
        }
    }

    public String desginStatusSensus(String id) {
        Integer integer = projectMapper.desginStatusSensus1(id);
        Integer integer1 = projectMapper.desginStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public String budgetStatusSensus(String id) {
        Integer integer = projectMapper.budgetStatusSensus1(id);
        Integer integer1 = projectMapper.budgetStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public String trackStatusSensus(String id) {
        Integer integer = projectMapper.trackStatusSensus1(id);
        Integer integer1 = projectMapper.trackStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public String visaStatusSensus(String id) {
        Integer integer = projectMapper.visaStatusSensus1(id);
        Integer integer1 = projectMapper.visaStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public String progressPaymentStatusSensus(String id) {
        Integer integer = projectMapper.progressPaymentStatusSensus1(id);
        Integer integer1 = projectMapper.progressPaymentStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public String settleAccountsStatusSensus(String id) {
        Integer integer = projectMapper.settleAccountsStatusSensus1(id);
        Integer integer1 = projectMapper.settleAccountsStatusSensus2(id);
        String s3 =
                "[{\"value1\":\"" + integer + "\",\"name1\":\"进行中\"},{\"value1\":\"" + integer1 + "\",\"name1\":\"已完成\"}]";
        return s3;
    }

    public Long buildDay(String id) throws ParseException {
        SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // 日期字符串
        String dateStr1 = projectMapper.buildingStartTime(id);
        String dateStr2 = projectMapper.buildingEndTime(id);

        // 获取日期
        Date start = DateFormat.parse(dateStr1);
        Date end = DateFormat.parse(dateStr2);

        // 获取相差的天数
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        long timeInMillis1 = calendar.getTimeInMillis();
        calendar.setTime(end);
        long timeInMillis2 = calendar.getTimeInMillis();

        long betweenDays = (timeInMillis2 - timeInMillis1) / (1000L * 3600L * 24L);
        System.out.println(betweenDays);
        return betweenDays;
    }

    public String projectCount(String id) {
        return projectMapper.projectCount(id);
    }

    public int missionCount(String id) {
        int count =
                projectMapper.settleAccountsStatusSensus1(id) + projectMapper.settleAccountsStatusSensus2(id) +
                        projectMapper.progressPaymentStatusSensus1(id) + projectMapper.progressPaymentStatusSensus2(id) +
                        projectMapper.visaStatusSensus1(id) + projectMapper.visaStatusSensus2(id) +
                        projectMapper.trackStatusSensus1(id) + projectMapper.trackStatusSensus2(id) +
                        +projectMapper.budgetStatusSensus1(id)
                        + projectMapper.budgetStatusSensus2(id)
                        + projectMapper.desginStatusSensus1(id)
                        + projectMapper.desginStatusSensus2(id);
        return count;
    }

    public BigDecimal desMoneySum(String id) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("buildingProjectId", id);
        BigDecimal officialReceipts = new BigDecimal(0);
        List<BaseProject> baseProjects = projectMapper.selectByExample(example);
        for (BaseProject baseProject : baseProjects) {
            DesignInfo designInfo = this.designInfoByid(baseProject.getId());
            if (designInfo != null) {
                AnhuiMoneyinfo anhuiMoneyinfo = this.anhuiMoneyinfoByid(designInfo.getId());
                WujiangMoneyInfo wujiangMoneyInfo = this.wujiangMoneyInfoByid(designInfo.getId());
                if (wujiangMoneyInfo != null) {
                    officialReceipts = anhuiMoneyinfo.getOfficialReceipts().add(officialReceipts);
                }
                if (anhuiMoneyinfo != null) {
                    officialReceipts = wujiangMoneyInfo.getOfficialReceipts().add(officialReceipts);
                }
            }
        }
        return officialReceipts.add(officialReceipts);
    }

    public BigDecimal outsourceMoneySum(String id) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("buildingProjectId", id);
        BigDecimal outsourceMoney = new BigDecimal(0);
        List<BaseProject> baseProjects = projectMapper.selectByExample(example);
        for (BaseProject baseProject : baseProjects) {
            DesignInfo designInfo = this.designInfoByid(baseProject.getId());
            if (designInfo != null) {
                if (designInfo.getOutsourceMoney() == null) {
                    designInfo.setOutsourceMoney(new BigDecimal(0));
                } else {
                    outsourceMoney.add(designInfo.getOutsourceMoney().add(outsourceMoney));
                }
            }
        }
        return outsourceMoney;
    }

    /**
     * 造价咨询费支出
     *
     * @param id
     * @return
     */
    public BigDecimal consultingExpenditure(String id) {
        //造价部门的委外金额
        BigDecimal bigDecimal = projectMapper.consultingExpenditure1(id);
        if (bigDecimal == null) {
            bigDecimal = new BigDecimal(0);
        }
        //造价部门的员工绩效
        BigDecimal bigDecimal1 = projectMapper.consultingExpenditure2(id);
        if (bigDecimal1 == null) {
            bigDecimal1 = new BigDecimal(0);
        }
        //两者相加就是支出
        return bigDecimal.add(bigDecimal1);
    }

    /**
     * 造价咨询费收入
     *
     * @param id
     * @return
     */
    public BigDecimal consultingIncome(String id) {
        BigDecimal bigDecimal = projectMapper.consultingIncome(id);
        if (bigDecimal == null) {
            bigDecimal = new BigDecimal(0);
        }
        return bigDecimal;
    }

    public BigDecimal costTotalAmountSum(String id) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("buildingProjectId", id);
        BigDecimal costTotalAmount = new BigDecimal(0);
        List<BaseProject> baseProjects = projectMapper.selectByExample(example);
        for (BaseProject baseProject : baseProjects) {
            CostPreparation costPreparation = this.costPreparationById(baseProject.getId());
            if (costPreparation != null) {
                if (costPreparation.getCostTotalAmount() != null) {
                    costTotalAmount = costPreparation.getCostTotalAmount().add(costTotalAmount);
                }
            }
        }
        return costTotalAmount;
    }

    public CostPreparation costPreparationById(String id) {
        Example example1 = new Example(CostPreparation.class);
        Example.Criteria c1 = example1.createCriteria();
        c1.andEqualTo("budgetingId", id);
        CostPreparation costPreparation = costPreparationDao.selectOneByExample(example1);
        return costPreparation;
    }

    public CostPreparation costPreparationById2(String id) {
        Example example1 = new Example(CostPreparation.class);
        Example.Criteria c1 = example1.createCriteria();
        c1.andEqualTo("baseProjectId", id);
        CostPreparation costPreparation = costPreparationDao.selectOneByExample(example1);
        return costPreparation;
    }

    public BigDecimal amountCostAmountSum(String id) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("buildingProjectId", id);
        BigDecimal costTotalAmount = new BigDecimal(0);
        List<BaseProject> baseProjects = projectMapper.selectByExample(example);
        for (BaseProject baseProject : baseProjects) {
            Example example1 = new Example(Budgeting.class);
            Example.Criteria c1 = example1.createCriteria();
            c1.andEqualTo("baseProjectId", baseProject.getId());
            Budgeting budgeting = budgetingMapper.selectOneByExample(example1);
            if (budgeting != null) {
                if (budgeting.getAmountCost() == null) {
                    budgeting.setAmountCost(new BigDecimal(0));
                } else {
                    costTotalAmount = budgeting.getAmountCost().add(costTotalAmount);
                }
            }
        }
        return costTotalAmount;
    }

    public BigDecimal biddingPriceControlSum(String id) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("buildingProjectId", id);
        BigDecimal costTotalAmount = new BigDecimal(0);
        List<BaseProject> baseProjects = projectMapper.selectByExample(example);
        for (BaseProject baseProject : baseProjects) {
            VeryEstablishment veryEstablishment = this.veryEstablishmentById(baseProject.getId());
            if (veryEstablishment != null) {
                if (veryEstablishment.getBiddingPriceControl() == null) {
                    veryEstablishment.setBiddingPriceControl(new BigDecimal(0));
                } else {
                    costTotalAmount = veryEstablishment.getBiddingPriceControl().add(costTotalAmount);
                }
            }
        }
        return costTotalAmount;
    }

    public VeryEstablishment veryEstablishmentById(String id) {
        Example example1 = new Example(VeryEstablishment.class);
        Example.Criteria c1 = example1.createCriteria();
        c1.andEqualTo("baseProjectId", id);
        VeryEstablishment veryEstablishment = veryEstablishmentDao.selectOneByExample(example1);
        return veryEstablishment;
    }

    public VeryEstablishment veryEstablishmentById2(String id) {
        Example example1 = new Example(VeryEstablishment.class);
        Example.Criteria c1 = example1.createCriteria();
        c1.andEqualTo("budgetingId", id);
        VeryEstablishment veryEstablishment = veryEstablishmentDao.selectOneByExample(example1);
        return veryEstablishment;
    }

    public List<MessageNotification> messageList(UserInfo userInfo) {
//        Example example = new Example(MessageNotification.class);
//        Example.Criteria c = example.createCriteria();
//        //todo userInfo.getId()
//        c.andEqualTo("acceptId", userInfo.getId());
//        List<MessageNotification> messageNotifications = messageNotificationDao.selectByExample(example);
        List<MessageNotification> message = messageNotificationDao.findMessage(userInfo.getId());
        return message;
    }

    public List<OneCensus> OneCensusList(CostVo2 costVo2) {
        List<OneCensus> oneCensuses = new ArrayList<>();
        //如果筛选时间为空
        if(costVo2.getStartTime()==null || costVo2.getStartTime().equals("")
                && costVo2.getEndTime() == null || costVo2.getEndTime().equals("")){
            //默认展示今年的
            CostVo2 costVo21 = this.NowYear(costVo2);
            oneCensuses = projectMapper.censusList(costVo21);
        }else{
            oneCensuses = projectMapper.censusList(costVo2);
        }
        return oneCensuses;
    }

    public PageInfo<BaseProject> individualList(IndividualVo individualVo) {
        PageHelper.startPage(individualVo.getPageNum(), individualVo.getPageSize());
        List<BaseProject> baseProjects = projectMapper.individualList(individualVo);
        PageInfo<BaseProject> baseProjectPageInfo = new PageInfo<>(baseProjects);
        return baseProjectPageInfo;
    }

    public Budgeting budgetingByid(String id) {
        Example example = new Example(Budgeting.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        Budgeting budgeting = budgetingMapper.selectOneByExample(example);
        return budgeting;
    }

    public TrackAuditInfo trackAuditInfoByid(String id) {
        Example example = new Example(TrackAuditInfo.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        TrackAuditInfo trackAuditInfo = trackAuditInfoDao.selectOneByExample(example);
        return trackAuditInfo;
    }

    public net.zlw.cloud.settleAccounts.model.SettlementAuditInformation SettlementAuditInformationByid(String id) {
        Example example = new Example(
                net.zlw.cloud.settleAccounts.model.SettlementAuditInformation.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        net.zlw.cloud.settleAccounts.model.SettlementAuditInformation settlementAuditInformation = settlementAuditInformationDao.selectOneByExample(example);
        return settlementAuditInformation;
    }

    public net.zlw.cloud.settleAccounts.model.LastSettlementReview lastSettlementReviewbyid(String id) {
        Example example = new Example(LastSettlementReview.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("baseProjectId", id);
        net.zlw.cloud.settleAccounts.model.LastSettlementReview lastSettlementReview = lastSettlementReviewDao.selectOneByExample(example);
        return lastSettlementReview;
    }

    public ProjectVo3 progressPaymentInformationSum(String id) {
        List<String> strings = progressPaymentInformationDao.NewcurrentPaymentInformation(id);
        List<String> strings1 = progressPaymentInformationDao.SumcurrentPaymentInformation(id);
        String s2 = progressPaymentInformationDao.cumulativePaymentTimes(id);
        String s3 = progressPaymentInformationDao.currentPaymentRatio(id);
        ProjectVo3 projectVo3 = new ProjectVo3();
        if (strings.size() > 0) {
            String s = strings.get(0);
            projectVo3.setNewcurrentPaymentInformation(s);

        } else {
            projectVo3.setNewcurrentPaymentInformation("-");
        }
        if (strings1.size() > 0) {
            String s1 = strings1.get(0);
            projectVo3.setSumcurrentPaymentInformation(s1);
        } else {
            projectVo3.setSumcurrentPaymentInformation("-");
        }

        projectVo3.setCumulativePaymentTimes(s2);
        projectVo3.setCurrentPaymentRatio(s3);
        return projectVo3;
    }

    public ProjectVo3 visaApplyChangeInformationSum(String id) {
        String s = visaApplyChangeInformationMapper.amountVisaChangeSum(id);
        String s1 = visaApplyChangeInformationMapper.changeCount(id);
        String s2 = visaApplyChangeInformationMapper.contractAmount(id);
        ProjectVo3 projectVo3 = new ProjectVo3();
        projectVo3.setAmountVisaChangeSum(s);
        projectVo3.setChangeCount(s1);
        projectVo3.setContractAmount(s2);
        return projectVo3;
    }

    /**
     * 应技提金额
     *
     * @return
     */
    public BigDecimal accruedAmount(BigDecimal desMoney) {
        BigDecimal multiply = desMoney.multiply(new BigDecimal(0.05));
        BigDecimal divide = multiply.divide(new BigDecimal(1.06), 2, BigDecimal.ROUND_HALF_UP);
        return divide;
    }

    /**
     * 建议计提金额
     */
    public BigDecimal proposedAmount(BigDecimal accruedAmount) {
        BigDecimal multiply = accruedAmount.multiply(new BigDecimal(0.8));
        return multiply;
    }

    /**
     * 余额
     *
     * @param accruedAmount
     * @param proposedAmount
     * @return
     */
    public BigDecimal surplus(BigDecimal accruedAmount, BigDecimal proposedAmount) {
        BigDecimal subtract = accruedAmount.subtract(proposedAmount);
        return subtract;
    }

    /**
     * 当前用户代办预算编制预算编制个数
     */
    public String budgetingCount(String id, String district) {
        String budgetingCount = projectMapper.budgetingCount(id, district);
        return budgetingCount;
    }

    /**
     * 当前用户代办进度款支付个数
     *
     * @param id
     * @return
     */
    public String progressPaymentInformationCount(String id, String district) {
        return projectMapper.progressPaymentInformationCount(id, district);
    }

    /**
     * 当前用户代办签证变更个数
     *
     * @param id
     * @return
     */
    public String visaApplyChangeInformationCount(String id, String district) {
        return projectMapper.visaApplyChangeInformationCount(id, district);
    }

    /**
     * 当前用户代办跟踪审计个数
     *
     * @param id
     * @return
     */
    public String trackAuditInfoCount(String id, String district) {
        return projectMapper.trackAuditInfoCount(id, district);
    }

    /**
     * 当前用户代办结算编制个数
     *
     * @param id
     * @return
     */
    public String settleAccountsCount(String id, String district) {
        return projectMapper.settleAccountsCount(id, district);
    }

    /**
     * 获取当前年份
     */
    public String getSysYear() {
        Calendar date = Calendar.getInstance();
        String year = String.valueOf(date.get(Calendar.YEAR));
        return year;
    }

    /**
     * 获取当前月份
     */
    public int getSysMouth() {
        Calendar date = Calendar.getInstance();
        int month = date.get(Calendar.MONTH) + 1;
        return month;
    }

    /**
     * 获取本年
     *
     * @param costVo2
     * @return
     */
    public CostVo2 NowYear(CostVo2 costVo2) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        costVo2.setYear(year + "");
        costVo2.setStartTime(year + "-01-01");
        costVo2.setEndTime(year + "-12-31");
        return costVo2;
    }

    /**
     * 获取本月
     *
     * @param costVo2
     * @return
     */
    public CostVo2 NowMonth(CostVo2 costVo2) {
        SimpleDateFormat sf = new SimpleDateFormat("dd");
        Calendar now = Calendar.getInstance();
        //当前年
        String year = String.valueOf(now.get(Calendar.YEAR));
        //当前月
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        //当前月最后一天
        //设置日期为本月最大日期
        now.set(Calendar.DATE, now.getActualMaximum(now.DATE));
        String day = sf.format(now.getTime());
        //开始时间 结束时间
        costVo2.setStartTime(year + "-" + month + "-" + "01");
        costVo2.setEndTime(year + "-" + month + "-" + day);
        return costVo2;
    }

    /**
     * 造价年表
     *
     * @param costVo2
     * @return
     */
    public OneCensus2 costCensus(CostVo2 costVo2) {
        //todo getid
//        costVo2.setId(userInfo.getId());
        if (costVo2.getStartTime() != null && !"".equals(costVo2.getStartTime())) {
            OneCensus2 oneCensus2 = projectMapper.costCensus(costVo2);
            return oneCensus2;
        } else {
            CostVo2 costVo21 = this.NowYear(costVo2);
            OneCensus2 oneCensus2 = projectMapper.costCensus(costVo21);
            return oneCensus2;
        }
    }

    /**
     * 造价月表
     *
     * @param costVo2
     * @return
     */
    public List<OneCensus2> costCensusList(CostVo2 costVo2) {
        //todo getLoginUser().getId()
        List<OneCensus2> oneCensus2s = null;
        if (costVo2.getStartTime() != null && !"".equals(costVo2.getStartTime())) {
            oneCensus2s = projectMapper.costCensusList(costVo2);
        } else {
            CostVo2 costVo21 = this.NowYear(costVo2);
            oneCensus2s = projectMapper.costCensusList(costVo21);
        }
        for (OneCensus2 oneCensus2 : oneCensus2s) {
            Integer budget = oneCensus2.getBudget();
            Integer track = oneCensus2.getTrack();
            Integer visa = oneCensus2.getVisa();
            Integer progresspayment = oneCensus2.getProgresspayment();
            Integer settleaccounts = oneCensus2.getSettleaccounts();
            Integer total = budget + track + visa + progresspayment + settleaccounts;
            oneCensus2.setTotal(total);
        }
        return oneCensus2s;
    }

    public Integer yearTaskCount(CostVo2 costVo2) {
        CostVo2 costVo21 = this.NowYear(costVo2);
        OneCensus2 oneCensus2 = projectMapper.costCensus(costVo2);
        Integer budget = oneCensus2.getBudget();
        Integer track = oneCensus2.getTrack();
        Integer visa = oneCensus2.getVisa();
        Integer progresspayment = oneCensus2.getProgresspayment();
        Integer settleaccounts = oneCensus2.getSettleaccounts();
        Integer total = budget + track + visa + progresspayment + settleaccounts;
        return total;
    }

    public Integer mouthDesCount(CostVo2 costVo2) {
        List<OneCensus> oneCensuses = projectMapper.censusList(costVo2);
        OneCensus oneCensus = oneCensuses.get(0);
        Integer municipalPipeline = oneCensus.getMunicipalPipeline();
        Integer networkReconstruction = oneCensus.getNetworkReconstruction();
        Integer newCommunity = oneCensus.getNewCommunity();
        Integer secondaryWater = oneCensus.getSecondaryWater();
        Integer commercialHouseholds = oneCensus.getCommercialHouseholds();
        Integer waterResidents = oneCensus.getWaterResidents();
        Integer administration = oneCensus.getAdministration();
        Integer total = municipalPipeline + networkReconstruction + newCommunity + secondaryWater + commercialHouseholds + waterResidents + administration;
        return total;
    }

    public Integer yearDesCount(CostVo2 costVo2) {
        Integer total = 0;
        List<OneCensus> oneCensuses = projectMapper.censusList(costVo2);
        for (OneCensus oneCensus : oneCensuses) {
            total += oneCensus.getMunicipalPipeline();
            total += oneCensus.getNetworkReconstruction();
            total += oneCensus.getNewCommunity();
            total += oneCensus.getSecondaryWater();
            total += oneCensus.getCommercialHouseholds();
            total += oneCensus.getWaterResidents();
            total += oneCensus.getAdministration();
        }
        return total;
    }

    public String buildSubmit(BuildingProject buildingProject) throws Exception {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        List<BuildingProject> nameAndCode = buildingProjectMapper.findNameAndCode(buildingProject.getBuildingProjectName(), buildingProject.getBuildingProjectCode());
        //判断如果decimal类型如果为空就设置为0
        if ("".equals(buildingProject.getCostAmount())) {
            buildingProject.setCostAmount("0");
        }
        if (nameAndCode.size() > 0) {
            throw new RuntimeException("建设名称或者编号重复");
        } else {
            buildingProject.setId(uuid);
            //完成状态
            buildingProject.setDelFlag("0");
            buildingProject.setMergeFlag("2");
            buildingProject.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            buildingProjectMapper.insertSelective(buildingProject);
        }

        return uuid;
    }

    public void updateSubmit(BuildingProject buildingProject) throws Exception {
        List<BuildingProject> nameAndCodeAndId = buildingProjectMapper.findNameAndCodeAndId(buildingProject.getId(), buildingProject.getBuildingProjectName(), buildingProject.getBuildingProjectCode());
        if ("".equals(buildingProject.getCostAmount())) {
            buildingProject.setCostAmount("0");
        }
        if (nameAndCodeAndId.size() > 0) {
            throw new Exception("建设名称或者编号重复");
        } else {
            System.err.println(buildingProject);
            System.err.println(buildingProject);
            System.err.println(buildingProject);
            System.err.println(buildingProject);
            System.err.println(buildingProject);
            System.err.println(buildingProject);

            buildingProjectMapper.updateByPrimaryKeySelective(buildingProject);
        }

    }

    public void updateFileInfo(FileInfo fileInfo) {
        fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
    }

    public BuildingProject findOne(String id) {
        return buildingProjectMapper.findOne(id);
    }

    public AuditInfo auditInfoByYes(UserInfo userInfo, String id) {
        Example example = new Example(AuditInfo.class);
        Example.Criteria criteria = example.createCriteria();
        //todo userInfo.getId()
        criteria.andEqualTo("auditorId", userInfo.getId());
        criteria.andEqualTo("baseProjectId", id);
        criteria.andEqualTo("auditResult", "0");
        AuditInfo auditInfo = auditInfoDao.selectOneByExample(example);
        return auditInfo;
    }

    public Integer designReviewedCount(UserInfo loginUser) {
        String id = loginUser.getId();
        return designInfoMapper.designReviewedCount(id);
    }

    public Integer designChangeReviewedCount(UserInfo loginUser) {
        String id = loginUser.getId();
        return designInfoMapper.designChangeReviewedCount(id);
    }

    public String findDesignUnit(String designUnit) {
        return designInfoMapper.findDesignUnit(designUnit);
    }

    public List<MkyUser> findCurrent(String id) {
      List<MkyUser> list =  auditInfoDao.findCurrent(id);
        System.err.println(list);
      return list;
    }

    public List<MkyUser> findCurrentCost(String id) {
      return   auditInfoDao.findCurrentCost(id);
    }
}
