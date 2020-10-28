package net.zlw.cloud.progressPayment.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import net.tec.cloud.common.bean.UserInfo;
import net.zlw.cloud.budgeting.model.vo.BatchReviewVo;
import net.zlw.cloud.designProject.mapper.BudgetingMapper;
import net.zlw.cloud.designProject.model.Budgeting;
import net.zlw.cloud.general.model.AuditChekedVo;
import net.zlw.cloud.index.model.vo.pageVo;
import net.zlw.cloud.maintenanceProjectInformation.mapper.ConstructionUnitManagementMapper;
import net.zlw.cloud.maintenanceProjectInformation.model.ConstructionUnitManagement;
import net.zlw.cloud.progressPayment.mapper.*;
import net.zlw.cloud.progressPayment.model.*;
import net.zlw.cloud.progressPayment.model.vo.BaseProjectVo;
import net.zlw.cloud.progressPayment.model.vo.PageVo;
import net.zlw.cloud.progressPayment.model.vo.ProgressListVo;
import net.zlw.cloud.progressPayment.model.vo.VisaBaseProjectVo;
import net.zlw.cloud.progressPayment.service.BaseProjectService;
import net.zlw.cloud.snsEmailFile.mapper.FileInfoMapper;
import net.zlw.cloud.snsEmailFile.model.FileInfo;
import net.zlw.cloud.statisticalAnalysis.model.vo.NumberVo;
import net.zlw.cloud.warningDetails.model.MemberManage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BaseProjectServiceimpl implements BaseProjectService {
    @Resource
    private BaseProjectDao baseProjectDao;
    @Resource
    private AuditInfoDao auditInfoDao;
    @Resource
    private ApplicationInformationDao applicationInformationDao;
    @Resource
    private ProgressPaymentInformationDao progressPaymentInformationDao;
    @Resource
    private ProgressPaymentTotalPaymentDao progressPaymentTotalPaymentDao;
    @Resource
    private MemberManageDao memberManageDao;

    @Resource
    private ConstructionUnitManagementMapper constructionUnitManagementMapper;

    @Resource
    private BudgetingMapper budgetingMapper;


    @Autowired
    private FileInfoMapper fileInfoMapper;
    /**
     * 添加进度款信息
     *
     * @param baseProject 用户填写信息表
     * @param loginUser
     */
    @Override
    public void addProgress(BaseProjectVo baseProject, UserInfo loginUser) {


//        Budgeting budgeting = budgetingMapper.selectByPrimaryKey(baseProject.getId());

        //项目基本信息
//        BaseProject project = findById(budgeting.getBaseProjectId());
        BaseProject project = baseProjectDao.selectByPrimaryKey(baseProject.getBaseId());
        //申请信息
        ApplicationInformation information = new ApplicationInformation();
        //进度款累计支付信息
        ProgressPaymentTotalPayment payment = new ProgressPaymentTotalPayment();
        //本期进度款支付信息
        ProgressPaymentInformation paymentInformation = new ProgressPaymentInformation();
        //审核表
        AuditInfo auditInfo = new AuditInfo();
        //成员管理


        paymentInformation.setCurrentPaymentInformation(baseProject.getCurrentPaymentInformation());
        paymentInformation.setCumulativePaymentTimes(baseProject.getCumulativePaymentTimes());
        paymentInformation.setCurrentPaymentRatio(baseProject.getCurrentPaymentRatio());
        paymentInformation.setCurrentPeriodAccording(baseProject.getCurrentPeriodAccording());
        paymentInformation.setContractAmount(baseProject.getContractAmount());
        paymentInformation.setProjectType(baseProject.getProjectType());
        paymentInformation.setReceivingTime(baseProject.getReceivingTime());
        paymentInformation.setCompileTime(baseProject.getCompileTime());
        paymentInformation.setOutsourcing(baseProject.getOutsourcing());
        paymentInformation.setNameOfCostUnit(baseProject.getNameOfCostUnit());
        paymentInformation.setContact(baseProject.getContact());
        paymentInformation.setContactPhone(baseProject.getContactPhone());
        paymentInformation.setAmountOutsourcing(baseProject.getAmountOutsourcing());
        paymentInformation.setSituation(baseProject.getSituation());
        paymentInformation.setRemarkes(baseProject.getRemarkes());
        paymentInformation.setBaseProjectId(project.getId());
        paymentInformation.setId(UUID.randomUUID().toString().replace("-",""));
        paymentInformation.setFounderId(loginUser.getId());
        paymentInformation.setDelFlag("0");

        String format = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").format(new Date());
        paymentInformation.setCreateTime(format);
        progressPaymentInformationDao.insert(paymentInformation);

        if (baseProject != null && !baseProject.equals("")) {
            if (baseProject.getAuditNumber() != null && !baseProject.getAuditNumber().equals("")) {
                project.setProgressPaymentStatus("1");
                project.setProjectFlow(project.getProjectFlow()+",4");
                baseProjectDao.updateByPrimaryKeySelective(project);
                auditInfo.setBaseProjectId(paymentInformation.getId());
                auditInfo.setAuditResult("0");
                auditInfo.setAuditorId(baseProject.getAuditorId());
                auditInfo.setCreateTime(format);
                auditInfo.setAuditType("0");
                auditInfo.setStatus("0");
                auditInfo.setId(UUID.randomUUID().toString().replace("-",""));
                auditInfoDao.insert(auditInfo);
            } else {
                project.setProgressPaymentStatus("2");
                project.setProjectFlow(project.getProjectFlow()+",4");
                baseProjectDao.updateByPrimaryKeySelective(project);
            }

            information.setRemarkes(baseProject.getRemarkes());
            information.setBaseProjectId(project.getId());
            information.setId(UUID.randomUUID().toString());
            information.setProgressPaymentId(paymentInformation.getId());
            information.setDelFlag("0");

            applicationInformationDao.insert(information);


            payment.setTotalPaymentAmount(baseProject.getTotalPaymentAmount());
            payment.setCumulativeNumberPayment(baseProject.getCumulativeNumberPayment());
            payment.setAccumulativePaymentProportion(baseProject.getAccumulativePaymentProportion());
            payment.setBaseProjectId(project.getId());
            payment.setProgressPaymentId(paymentInformation.getId());
            payment.setId(UUID.randomUUID().toString());
            progressPaymentTotalPaymentDao.insert(payment);


            // 上传文件，新建-申请信息，列表文件
            List<FileInfo> byFreignAndType = fileInfoMapper.findByFreignAndType(baseProject.getKey(), baseProject.getType());

            for (FileInfo fileInfo : byFreignAndType) {
                fileInfo.setPlatCode(paymentInformation.getId());

                fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
            }

            // 本期进度款支付信息列表文件
            List<FileInfo> freignAndType = fileInfoMapper.findByFreignAndType(baseProject.getKey(), baseProject.getType1());

            for (FileInfo fileInfo : freignAndType) {
                fileInfo.setPlatCode(paymentInformation.getId());

                fileInfoMapper.updateByPrimaryKeySelective(fileInfo);
            }


        }
    }

    /**
     * 根据id查询进度款信息
     */
    @Override
    public BaseProjectVo seachProgressById(String id) {
        /*
        * //项目基本信息
        BaseProject project = new BaseProject();
        //申请信息
        ApplicationInformation information = new ApplicationInformation();
        //进度款累计支付信息
        ProgressPaymentTotalPayment payment = new ProgressPaymentTotalPayment();
        //本期进度款支付信息
        ProgressPaymentInformation paymentInformation = new ProgressPaymentInformation();
        //审核表
        AuditInfo auditInfo = new AuditInfo();
        * */
        BaseProjectVo baseProjectVo = new BaseProjectVo();
        ProgressPaymentInformation paymentInformation = progressPaymentInformationDao.selectByPrimaryKey(id);

        BaseProject baseProject = baseProjectDao.selectByPrimaryKey(paymentInformation.getBaseProjectId());

        // 查询审核信息的集合
        Example auditExample = new Example(AuditInfo.class);
        Example.Criteria criteria = auditExample.createCriteria();
        criteria.andEqualTo("baseProjectId",paymentInformation.getId());
        criteria.andEqualTo("status","0");
        List<AuditInfo> auditInfos = auditInfoDao.selectByExample(auditExample);

        for (AuditInfo auditInfo : auditInfos) {
            if("0".equals(auditInfo.getAuditResult())){
                baseProjectVo.setAuditType(auditInfo.getAuditType());
            }
        }


        Example example = new Example(ApplicationInformation.class);
        example.createCriteria().andEqualTo("progressPaymentId", id);
        ApplicationInformation applicationInformation = applicationInformationDao.selectOneByExample(example);

        Example example1 = new Example(ProgressPaymentTotalPayment.class);
        example1.createCriteria().andEqualTo("progressPaymentId", id);
        ProgressPaymentTotalPayment totalPayment = progressPaymentTotalPaymentDao.selectOneByExample(example1);


        baseProjectVo.setId(id);
        baseProjectVo.setProjectNum(baseProject.getProjectNum());
        baseProjectVo.setProjectName(baseProject.getProjectName());
        baseProjectVo.setApplicationNum(baseProject.getApplicationNum());
        baseProjectVo.setCeaNum(baseProject.getCeaNum());
        baseProjectVo.setDistrict(baseProject.getDistrict());
        baseProjectVo.setDesignCategory(baseProject.getDesignCategory());
        baseProjectVo.setConstructionUnit(baseProject.getConstructionUnit());
        baseProjectVo.setContacts(baseProject.getContacts());
        baseProjectVo.setContactNumber(baseProject.getContactNumber());
        baseProjectVo.setCustomerName(baseProject.getCustomerName());
        baseProjectVo.setSubject(baseProject.getSubject());
        baseProjectVo.setCustomerPhone(baseProject.getCustomerPhone());
        baseProjectVo.setConstructionOrganization(baseProject.getConstructionOrganization());
        baseProjectVo.setProjectNature(baseProject.getProjectNature());
        baseProjectVo.setProjectCategory(baseProject.getProjectCategory());
        baseProjectVo.setWaterAddress(baseProject.getWaterAddress());
        baseProjectVo.setWaterSupplyType(baseProject.getWaterSupplyType());
        baseProjectVo.setThisDeclaration(baseProject.getThisDeclaration());
        baseProjectVo.setAgent(baseProject.getAgent());
        baseProjectVo.setAgentPhone(baseProject.getAgentPhone());
        baseProjectVo.setApplicationDate(baseProject.getApplicationDate());
        baseProjectVo.setBusinessLocation(baseProject.getBusinessLocation());
        baseProjectVo.setBusinessTypes(baseProject.getBusinessTypes());
        baseProjectVo.setAB(baseProject.getAB());
        baseProjectVo.setWaterUse(baseProject.getWaterUse());
        baseProjectVo.setFireTableSize(baseProject.getFireTableSize());
        baseProjectVo.setClassificationCaliber(baseProject.getClassificationCaliber());
        baseProjectVo.setWaterMeterDiameter(baseProject.getWaterMeterDiameter());
        baseProjectVo.setSite(baseProject.getSite());
        baseProjectVo.setSystemNumber(baseProject.getSystemNumber());
        baseProjectVo.setProposer(baseProject.getProposer());
        baseProjectVo.setApplicationNumber(baseProject.getApplicationNumber());


        baseProjectVo.setRemarkes(applicationInformation.getRemarkes());


        baseProjectVo.setTotalPaymentAmount(totalPayment.getTotalPaymentAmount());
        baseProjectVo.setCumulativeNumberPayment(totalPayment.getCumulativeNumberPayment());
        baseProjectVo.setAccumulativePaymentProportion(totalPayment.getAccumulativePaymentProportion());


        baseProjectVo.setCurrentPaymentInformation(paymentInformation.getCurrentPaymentInformation());
        baseProjectVo.setCumulativePaymentTimes(paymentInformation.getCumulativePaymentTimes());
        baseProjectVo.setCurrentPaymentRatio(paymentInformation.getCurrentPaymentRatio());
        baseProjectVo.setCurrentPeriodAccording(paymentInformation.getCurrentPeriodAccording());
        baseProjectVo.setContractAmount(paymentInformation.getContractAmount());
        baseProjectVo.setProjectType(paymentInformation.getProjectType());
        baseProjectVo.setReceivingTime(paymentInformation.getReceivingTime());
        baseProjectVo.setCompileTime(paymentInformation.getCompileTime());
        baseProjectVo.setOutsourcing(paymentInformation.getOutsourcing());
        baseProjectVo.setNameOfCostUnit(paymentInformation.getNameOfCostUnit());
        baseProjectVo.setContact(paymentInformation.getContact());
        baseProjectVo.setContactPhone(paymentInformation.getContactPhone());
        baseProjectVo.setAmountOutsourcing(paymentInformation.getAmountOutsourcing());
        baseProjectVo.setSituation(paymentInformation.getSituation());
        baseProjectVo.setRemarkes(paymentInformation.getRemarkes());
        // TODO  审核信息
        baseProjectVo.setAuditInfos(auditInfos);

        Example example2 = new Example(AuditInfo.class);
        Example.Criteria criteria1 = example2.createCriteria();
        criteria1.andEqualTo("baseProjectId",id);

        List<AuditInfo> auditInfos1 = auditInfoDao.selectByExample(example2);

        for (AuditInfo auditInfo : auditInfos1) {
            if("0".equals(auditInfo.getAuditResult()) && ("0".equals(auditInfo.getAuditType()))){
                baseProjectVo.setAuditNumber("1");
            }else if("0".equals(auditInfo.getAuditResult()) && ("1".equals(auditInfo.getAuditType()))){
                baseProjectVo.setAuditNumber("2");
            }
        }

        return baseProjectVo;
    }

    //修改进度款
    @Override
    public void updateProgress(BaseProjectVo baseProject) {
        //项目基本信息
        Example example2 = new Example(BaseProject.class);
        example2.createCriteria().andEqualTo("projectNum",baseProject.getProjectNum());
        BaseProject project = baseProjectDao.selectOneByExample(example2);
        //申请信息
        ApplicationInformation information = new ApplicationInformation();
        //进度款累计支付信息
        ProgressPaymentTotalPayment payment = new ProgressPaymentTotalPayment();
        //本期进度款支付信息
        ProgressPaymentInformation paymentInformation = progressPaymentInformationDao.selectByPrimaryKey(baseProject.getId());
        //审核表
        AuditInfo auditInfo = new AuditInfo();


        if (baseProject != null && !baseProject.equals("")) {
            if (baseProject.getAuditNumber() != null && !baseProject.getAuditNumber().equals("")) {
                if (baseProject.getAuditNumber().equals("1")){
                    project.setAuditNumber("1");
                    baseProjectDao.updateByPrimaryKey(project);
                    auditInfo.setBaseProjectId(baseProject.getId());
                    auditInfo.setAuditResult("0");
                    auditInfo.setAuditorId(baseProject.getAuditorId());
                    auditInfo.setId(UUID.randomUUID().toString().replace("-",""));
                    auditInfo.setAuditType("1");
                    auditInfoDao.insert(auditInfo);
                }else if(baseProject.getAuditNumber().equals("2")){
                    Example example = new Example(AuditInfo.class);
                    example.createCriteria().andEqualTo("baseProjectId",baseProject.getId());
                    List<AuditInfo> auditInfos = auditInfoDao.selectByExample(example);
                    for (AuditInfo info : auditInfos) {
                        if (info.getAuditResult().equals("0")){
                            info.setAuditResult("1");
                            auditInfoDao.updateByPrimaryKeySelective(info);
                            break;
                        }
                    }
                }else if(baseProject.getAuditNumber().equals("0")){// 如果是 0，代表处理中状态
                    project.setProgressPaymentStatus("1");
                    project.setProjectFlow(project.getProjectFlow()+",4");
                    baseProjectDao.updateByPrimaryKeySelective(project);
                    auditInfo.setBaseProjectId(paymentInformation.getId());
                    auditInfo.setAuditResult("0");
                    auditInfo.setAuditorId(baseProject.getAuditorId());
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
                    String format = simpleDateFormat.format(new Date());
                    auditInfo.setCreateTime(format);
                    auditInfo.setAuditType("0");
                    auditInfo.setStatus("0");
                    auditInfo.setId(UUID.randomUUID().toString().replace("-",""));
                    auditInfoDao.insert(auditInfo);
                }
            } else {
                    project.setProgressPaymentStatus("2");
                    baseProjectDao.updateByPrimaryKeySelective(project);
            }







            information.setRemarkes(baseProject.getRemarkes());
            Example example = new Example(ApplicationInformation.class);
            example.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
            applicationInformationDao.updateByExampleSelective(information,example);


            payment.setTotalPaymentAmount(baseProject.getTotalPaymentAmount());
            payment.setCumulativeNumberPayment(baseProject.getCumulativeNumberPayment());
            payment.setAccumulativePaymentProportion(baseProject.getAccumulativePaymentProportion());
            Example example1 = new Example(ProgressPaymentTotalPayment.class);
            example1.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
            progressPaymentTotalPaymentDao.updateByExampleSelective(payment,example1);

            paymentInformation.setCurrentPaymentInformation(baseProject.getCurrentPaymentInformation());
            paymentInformation.setCumulativePaymentTimes(baseProject.getCumulativePaymentTimes());
            paymentInformation.setCurrentPaymentRatio(baseProject.getCurrentPaymentRatio());
            paymentInformation.setCurrentPeriodAccording(baseProject.getCurrentPeriodAccording());
            paymentInformation.setContractAmount(baseProject.getContractAmount());
            paymentInformation.setProjectType(baseProject.getProjectType());
            paymentInformation.setReceivingTime(baseProject.getReceivingTime());
            paymentInformation.setCompileTime(baseProject.getCompileTime());
            paymentInformation.setOutsourcing(baseProject.getOutsourcing());
            paymentInformation.setNameOfCostUnit(baseProject.getNameOfCostUnit());
            paymentInformation.setContact(baseProject.getContact());
            paymentInformation.setContactPhone(baseProject.getContactPhone());
            paymentInformation.setAmountOutsourcing(baseProject.getAmountOutsourcing());
            paymentInformation.setSituation(baseProject.getSituation());
            paymentInformation.setRemarkes(baseProject.getRemarkes());
            Example example3 = new Example(ProgressPaymentInformation.class);
            example3.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
            progressPaymentInformationDao.updateByExampleSelective(paymentInformation,example3);
        }
    }


    // 未通過編輯
    public void updateProgressPayment(BaseProjectVo baseProject){

        //申请信息
        ApplicationInformation information = new ApplicationInformation();
        //进度款累计支付信息
        ProgressPaymentTotalPayment payment = new ProgressPaymentTotalPayment();
        //本期进度款支付信息
        ProgressPaymentInformation paymentInformation = progressPaymentInformationDao.selectByPrimaryKey(baseProject.getId());
        //审核表
        AuditInfo auditInfo = new AuditInfo();

        //项目基本信息
        BaseProject project = baseProjectDao.selectByPrimaryKey(paymentInformation.getBaseProjectId());

        Example example = new Example(AuditInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("baseProjectId", paymentInformation.getId());

        List<AuditInfo> auditInfos = auditInfoDao.selectByExample(example);

        for (AuditInfo info : auditInfos) {
            // 不通過,一審
            if("2".equals(info.getAuditResult()) && "0".equals(info.getAuditType())){

                info.setAuditResult("0");
                info.setBaseProjectId(paymentInformation.getId());
                String updateDate = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").format(new Date());

                info.setUpdateTime(updateDate);

                project.setProgressPaymentStatus("1");

                auditInfoDao.updateByPrimaryKeySelective(info);
                baseProjectDao.updateByPrimaryKeySelective(project);

            }else if("2".equals(info.getAuditResult()) && "1".equals(info.getAuditType())){
                auditInfoDao.deleteByPrimaryKey(info);

                Example example1 = new Example(AuditInfo.class);

                Example.Criteria criteria1 = example1.createCriteria();

                criteria1.andEqualTo("baseProjectId",paymentInformation.getId()).andEqualTo("auditType","0");

                AuditInfo auditInfo1 = auditInfoDao.selectOneByExample(example1);

                auditInfo1.setAuditResult("0");

                auditInfoDao.updateByPrimaryKeySelective(auditInfo1);
                project.setProgressPaymentStatus("1");

                baseProjectDao.updateByPrimaryKeySelective(project);
            }
        }
        information.setRemarkes(baseProject.getRemarkes());
        Example example3 = new Example(ApplicationInformation.class);
        example3.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
        applicationInformationDao.updateByExampleSelective(information,example3);


        payment.setTotalPaymentAmount(baseProject.getTotalPaymentAmount());
        payment.setCumulativeNumberPayment(baseProject.getCumulativeNumberPayment());
        payment.setAccumulativePaymentProportion(baseProject.getAccumulativePaymentProportion());
        Example example1 = new Example(ProgressPaymentTotalPayment.class);
        example1.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
        progressPaymentTotalPaymentDao.updateByExampleSelective(payment,example1);

        paymentInformation.setCurrentPaymentInformation(baseProject.getCurrentPaymentInformation());
        paymentInformation.setCumulativePaymentTimes(baseProject.getCumulativePaymentTimes());
        paymentInformation.setCurrentPaymentRatio(baseProject.getCurrentPaymentRatio());
        paymentInformation.setCurrentPeriodAccording(baseProject.getCurrentPeriodAccording());
        paymentInformation.setContractAmount(baseProject.getContractAmount());
        paymentInformation.setProjectType(baseProject.getProjectType());
        paymentInformation.setReceivingTime(baseProject.getReceivingTime());
        paymentInformation.setCompileTime(baseProject.getCompileTime());
        paymentInformation.setOutsourcing(baseProject.getOutsourcing());
        paymentInformation.setNameOfCostUnit(baseProject.getNameOfCostUnit());
        paymentInformation.setContact(baseProject.getContact());
        paymentInformation.setContactPhone(baseProject.getContactPhone());
        paymentInformation.setAmountOutsourcing(baseProject.getAmountOutsourcing());
        paymentInformation.setSituation(baseProject.getSituation());
        paymentInformation.setRemarkes(baseProject.getRemarkes());
        Example example4 = new Example(ProgressPaymentInformation.class);
        example4.createCriteria().andEqualTo("progressPaymentId",paymentInformation.getId());
        progressPaymentInformationDao.updateByExampleSelective(paymentInformation,example4);


    }


    @Override
    public void batchReview(BatchReviewVo batchReviewVo) {
        batchReviewVo.getBatchAll();
        String[] split = batchReviewVo.getBatchAll().split(",");
        for (String s : split) {
            Example example = new Example(AuditInfo.class);
            example.createCriteria().andEqualTo("baseProjectId",s).andEqualTo("auditResult","0");
            AuditInfo auditInfo = auditInfoDao.selectOneByExample(example);

            ProgressPaymentInformation progressPaymentInformation = progressPaymentInformationDao.selectByPrimaryKey(s);
            String baseProjectId = progressPaymentInformation.getBaseProjectId();

            BaseProject baseProject = baseProjectDao.selectByPrimaryKey(baseProjectId);


            if (batchReviewVo.getAuditResult().equals("1")){
                if (auditInfo.getAuditType().equals("0")){
                    auditInfo.setAuditResult("1");
                    Date date = new Date();
                    String format = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").format(date);
                    auditInfo.setAuditTime(format);
                    auditInfo.setAuditOpinion(batchReviewVo.getAuditOpinion());
                    auditInfoDao.updateByPrimaryKeySelective(auditInfo);
                    AuditInfo auditInfo1 = new AuditInfo();
                    auditInfo1.setId(UUID.randomUUID().toString().replace("-",""));
                    auditInfo1.setBaseProjectId(s);
                    auditInfo1.setAuditResult("0");
                    auditInfo1.setAuditType("1");

                    baseProject.setProgressPaymentStatus("4");
                    baseProjectDao.updateByPrimaryKeySelective(baseProject);

                    Example example3 = new Example(MemberManage.class);
                    example3.createCriteria().andEqualTo("status", "0").andEqualTo("depId", "2").andEqualTo("depAdmin", "1");
                    MemberManage memberManage = memberManageDao.selectOneByExample(example3);

                    auditInfo1.setAuditorId(memberManage.getId());

                    auditInfoDao.insertSelective(auditInfo1);
                }else if(auditInfo.getAuditType().equals("1")){
                    auditInfo.setAuditResult("1");
                    Date date = new Date();
                    String format = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").format(date);
                    auditInfo.setAuditTime(format);
                    auditInfo.setAuditOpinion(batchReviewVo.getAuditOpinion());

                    baseProject.setProgressPaymentStatus("5");
                    baseProjectDao.updateByPrimaryKeySelective(baseProject);

                    auditInfoDao.updateByPrimaryKeySelective(auditInfo);
                }
            }else if(batchReviewVo.getAuditResult().equals("2")){
                auditInfo.setAuditResult("2");
                auditInfo.setAuditOpinion(batchReviewVo.getAuditOpinion());
                Date date = new Date();
                String format = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").format(date);
                auditInfo.setAuditTime(format);

                baseProject.setProgressPaymentStatus("3");

                baseProjectDao.updateByPrimaryKeySelective(baseProject);
                auditInfoDao.updateByPrimaryKeySelective(auditInfo);
            }
        }
    }



    @Override
    public List<BaseProject> findBaseProject(String name) {
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        if (name.equals(" ") || name==null){
            List<BaseProject> baseProjects = baseProjectDao.selectAll();
            return baseProjects;
        }
        c.andLike("projectNum","%"+name+"%");
        List<BaseProject> baseProjects = baseProjectDao.selectByExample(example);
        return baseProjects;
    }

    @Override
    public List<BaseProject> findAllBaseProject(pageVo pageVo) {
        return baseProjectDao.findAllBaseProject(pageVo);
    }

    @Override
    public BaseProject findById(String id) {

        Budgeting budgeting = budgetingMapper.selectByPrimaryKey(id);
        BaseProject baseProject = new BaseProject();
        if(budgeting != null){
            String baseProjectId = budgeting.getBaseProjectId();
            baseProject = baseProjectDao.selectByPrimaryKey(baseProjectId);
        }else{
            baseProject = baseProjectDao.selectByPrimaryKey(id);
        }
        if(baseProject != null){
            ConstructionUnitManagement constructionUnitManagement = constructionUnitManagementMapper.selectById(baseProject.getConstructionUnit());
            if (constructionUnitManagement!=null){
                baseProject.setConstructionOrganization(constructionUnitManagement.getConstructionUnitName());
            }
        }

        return baseProject;
    }

    @Override
    public NumberVo NumberItems() {
        NumberVo numberVo = new NumberVo();
        int i = baseProjectDao.selectCountByExample(null);
        numberVo.setTotalNumberOfProjects(i);


        int ii = 0;
        Example example = new Example(BaseProject.class);
        Example.Criteria c = example.createCriteria();
        c.andEqualTo("desginStatus","1");
        int i1 = baseProjectDao.selectCountByExample(example);
        ii+=i1;

        Example example2 = new Example(BaseProject.class);
        Example.Criteria c2 = example2.createCriteria();
        c2.andEqualTo("budgetStatus","1");
        int i2 = baseProjectDao.selectCountByExample(example2);
        ii+=i2;

        Example example3 = new Example(BaseProject.class);
        Example.Criteria c3 = example3.createCriteria();
        c3.andEqualTo("trackStatus","1");
        int i3 = baseProjectDao.selectCountByExample(example3);
        ii+=i3;


        Example example5 = new Example(BaseProject.class);
        Example.Criteria c5 = example5.createCriteria();
        c5.andEqualTo("visaStatus","1");
        int i5 = baseProjectDao.selectCountByExample(example5);
        ii+=i5;

        Example example6 = new Example(BaseProject.class);
        Example.Criteria c6 = example6.createCriteria();
        c6.andEqualTo("progressPaymentStatus","1");
        int i6 = baseProjectDao.selectCountByExample(example6);
        ii+=i6;

        Example example7 = new Example(BaseProject.class);
        Example.Criteria c7 = example7.createCriteria();
        c7.andEqualTo("settleAccountsStatus","1");
        int i7 = baseProjectDao.selectCountByExample(example7);
        ii+=i7;
        numberVo.setTotalNumberOfApproval(ii);




        int iii = 0;
        Example aexample = new Example(BaseProject.class);
        Example.Criteria ac = aexample.createCriteria();
        ac.andEqualTo("desginStatus","2");
        int ai1 = baseProjectDao.selectCountByExample(aexample);
        iii+=ai1;

        Example aexample2 = new Example(BaseProject.class);
        Example.Criteria ac2 = aexample2.createCriteria();
        ac2.andEqualTo("budgetStatus","2");
        int ai2 = baseProjectDao.selectCountByExample(aexample2);
        iii+=ai2;

        Example aexample3 = new Example(BaseProject.class);
        Example.Criteria ac3 = aexample3.createCriteria();
        ac3.andEqualTo("trackStatus","3");
        int ai3 = baseProjectDao.selectCountByExample(aexample3);
        iii+=ai3;



        Example aexample5 = new Example(BaseProject.class);
        Example.Criteria ac5 = aexample5.createCriteria();
        ac5.andEqualTo("visaStatus","5");
        int ai5 = baseProjectDao.selectCountByExample(aexample5);
        iii+=ai5;

        Example aexample6 = new Example(BaseProject.class);
        Example.Criteria ac6 = aexample6.createCriteria();
        ac6.andEqualTo("progressPaymentStatus","2");
        int ai6 = baseProjectDao.selectCountByExample(aexample6);
        iii+=ai6;

        Example aexample7 = new Example(BaseProject.class);
        Example.Criteria ac7 = aexample7.createCriteria();
        ac7.andEqualTo("settleAccountsStatus","2");
        int ai7 = baseProjectDao.selectCountByExample(aexample7);
        iii+=ai7;
        numberVo.setProjectTotal(iii);



        int iiii = 0;
        Example cexample = new Example(BaseProject.class);
        Example.Criteria cc = cexample.createCriteria();
        cc.andEqualTo("desginStatus","2");
        int ci1 = baseProjectDao.selectCountByExample(cexample);
        iiii+=ci1;

        Example cexample2 = new Example(BaseProject.class);
        Example.Criteria cc2 = cexample2.createCriteria();
        cc2.andEqualTo("budgetStatus","2");
        int ci2 = baseProjectDao.selectCountByExample(cexample2);
        iiii+=ci2;

        Example cexample3 = new Example(BaseProject.class);
        Example.Criteria cc3 = cexample3.createCriteria();
        cc3.andEqualTo("trackStatus","3");
        int ci3 = baseProjectDao.selectCountByExample(cexample3);
        iiii+=ci3;



        Example cexample5 = new Example(BaseProject.class);
        Example.Criteria cc5 = cexample5.createCriteria();
        cc5.andEqualTo("visaStatus","5");
        int ci5 = baseProjectDao.selectCountByExample(cexample5);
        iiii+=ci5;

        Example cexample6 = new Example(BaseProject.class);
        Example.Criteria cc6 = cexample6.createCriteria();
        cc6.andEqualTo("progressPaymentStatus","2");
        int ci6 = baseProjectDao.selectCountByExample(aexample6);
        iiii+=ci6;

        Example cexample7 = new Example(BaseProject.class);
        Example.Criteria cc7 = aexample7.createCriteria();
        cc7.andEqualTo("settleAccountsStatus","2");
        int ci7 = baseProjectDao.selectCountByExample(cexample7);
        iiii+=ci7;
        numberVo.setTotalNumberOfCompleted(iiii);


        return numberVo;
    }

    @Override
    public PageInfo<ProgressListVo> searchAllProgress(PageVo pageVo) {
        // 设置分页助手
        PageHelper.startPage(pageVo.getPageNum(),pageVo.getPageSize());
        List<ProgressListVo> progressListVos = progressPaymentInformationDao.searchAllProgress(pageVo);
        List<ProgressListVo> progressListVos1 = progressPaymentInformationDao.searchAllProgress1(pageVo);

        PageInfo<ProgressListVo> progressListVoPageInfo = new PageInfo<>();

        // 全部状态
        if("".equals(pageVo.getProgressStatus())){
            progressListVoPageInfo = new PageInfo<>(progressListVos1);
        }else if("1".equals(pageVo.getProgressStatus()) || "3".equals(pageVo.getProgressStatus())){
            progressListVoPageInfo = new PageInfo<>(progressListVos);
        }else{
            progressListVoPageInfo = new PageInfo<>(progressListVos1);
        }

        return progressListVoPageInfo;
    }



    @Override
    public void deleteProgress(String id) {
        ProgressPaymentInformation progressPaymentInformation = new ProgressPaymentInformation();
        progressPaymentInformation.setDelFlag("1");
        progressPaymentInformation.setId(id);
        progressPaymentInformationDao.updateByPrimaryKeySelective(progressPaymentInformation);

        Example example = new Example(ProgressPaymentTotalPayment.class);
        example.createCriteria().andEqualTo("progressPaymentId",id);
        ProgressPaymentTotalPayment progressPaymentTotalPayment = new ProgressPaymentTotalPayment();
        progressPaymentTotalPayment.setDelFlag("1");
        progressPaymentTotalPaymentDao.updateByExampleSelective(progressPaymentTotalPayment,example);

        Example example1 = new Example(ApplicationInformation.class);
        example1.createCriteria().andEqualTo("progressPaymentId",id);
        ApplicationInformation applicationInformation = new ApplicationInformation();
        applicationInformation.setDelFlag("1");
        applicationInformationDao.updateByExampleSelective(applicationInformation,example1);

        Example example2 = new Example(AuditInfo.class);
        example2.createCriteria().andEqualTo("baseProjectId",id);
        auditInfoDao.deleteByExample(example2);
    }

    /***
     * 签证变更所选项目
     * @param visaBaseProjectVo
     * @return
     */
    @Override
    public List<VisaBaseProjectVo> selectByBaseProjectId(VisaBaseProjectVo visaBaseProjectVo) {
        PageHelper.startPage(visaBaseProjectVo.getPageNum(),visaBaseProjectVo.getPageSize());

        return baseProjectDao.findByBaseProject(visaBaseProjectVo);

    }

    @Override
    public void updateProject(BaseProject baseProject) {
        baseProjectDao.updateByPrimaryKeySelective(baseProject);
    }

    @Override
    public List<BaseProject> findByBuildingProject(String id) {
        return baseProjectDao.findByBuildingProject(id);
    }

    @Override
    public BaseProject findByBaseProjectId(VisaBaseProjectVo visaBaseProjectVo) {
        return baseProjectDao.selectByPrimaryKey(visaBaseProjectVo.getId());

    }

    @Override
    public BaseProject findByBuilding(String id) {
        Budgeting byId = budgetingMapper.findById(id);

        Example example = new Example(BaseProject.class);
        example.createCriteria().andEqualTo("id",byId.getBaseProjectId());
        BaseProject baseProject = baseProjectDao.selectOneByExample(example);

        if(baseProject != null){
            ConstructionUnitManagement constructionUnitManagement = constructionUnitManagementMapper.selectById(baseProject.getConstructionUnit());
            if (constructionUnitManagement!=null){
                baseProject.setConstructionOrganization(constructionUnitManagement.getConstructionUnitName());
            }
        }
        return baseProject;
    }

    @Override
    public List<AuditChekedVo> auditChek(String id) {
      return  auditInfoDao.auditChek(id);
    }
}
