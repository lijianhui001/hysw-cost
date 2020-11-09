package net.zlw.cloud.clearProject.model;

import java.io.Serializable;
import javax.persistence.*;
import lombok.Data;

@Data
@Table(name = "call_for_bids")
public class CallForBids implements Serializable {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "bid_project_num")
    private String bidProjectNum;

    @Column(name = "bid_project_name")
    private String bidProjectName;

    @Column(name = "bid_winner")
    private String bidWinner;

    @Column(name = "bid_money")
    private String bidMoney;

    @Column(name = "remark")
    private String remark;

    @Column(name = "clear_project_id")
    private String clearProjectId;

    @Column(name = "status")
    private String status;

    private static final long serialVersionUID = 1L;
}