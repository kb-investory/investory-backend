package com.investory.market.infra.clients.kis;

import lombok.Data;

@Data
public class KisStockInfoResponse {
    private String rt_cd;
    private String msg_cd;
    private String msg1;
    private Output output;

    @Data
    public static class Output {
        private String pdno;
        private String prdt_name;
        private String prdt_abrv_name;
        private String idx_bztp_lcls_cd;
        private String idx_bztp_lcls_cd_name;
        private String idx_bztp_mcls_cd;
        private String idx_bztp_mcls_cd_name;
        private String idx_bztp_scls_cd;
        private String idx_bztp_scls_cd_name;
        private String std_idst_clsf_cd;
        private String std_idst_clsf_cd_name;
        private String scts_mket_lstg_dt;
        private String scts_mket_lstg_abol_dt;
        private String kosdaq_mket_lstg_dt;
        private String kosdaq_mket_lstg_abol_dt;
    }
}
