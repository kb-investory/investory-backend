package com.investory.market.infra.clients.kis;

import lombok.Data;

@Data
public class KisPriceResponse {
    private String rt_cd;
    private String msg_cd;
    private String msg1;
    private Output output;

    @Data
    public static class Output {
        private String stck_lwpr;
        private String stck_hgpr;
        private String stck_oprc;
        private String stck_prpr;
        private String prdy_ctrt;
        private String acml_vol;
        private String acml_tr_pbmn;
    }
}
