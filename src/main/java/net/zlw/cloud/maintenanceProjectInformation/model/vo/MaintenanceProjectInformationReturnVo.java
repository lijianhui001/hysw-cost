package net.zlw.cloud.maintenanceProjectInformation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.access.method.P;

import java.math.BigDecimal;

/**
 * @Author dell
 * @Date 2020/10/11 14:29
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceProjectInformationReturnVo {
    private String id;
    //维修项目编号
    private String maintenanceItemId;
    //维修项目名称
    private String maintenanceItemName;
    //维修项目类型
    private String maintenanceItemType;

    private String type;
    private String submitTime;
    private String preparePeople;
    private BigDecimal reviewAmount;
    private String customerName;
    private String constructionUnitName;
    private String waterAddress;
    private String contractAmount;
    private String district;
    private String compileTime;
}