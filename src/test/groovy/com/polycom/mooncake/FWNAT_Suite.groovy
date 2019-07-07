package com.polycom.mooncake

import com.polycom.mooncake.FWNAT.RPAD_Keepalive
import com.polycom.mooncake.FWNAT.RPAD_MP_AVC_Bridge_RMX
import com.polycom.mooncake.FWNAT.RPAD_MP_AVC_VEQ_RMX
import com.polycom.mooncake.FWNAT.RPAD_MP_AVC_VMR_Guest_And_Remote_User_Only_RMX
import com.polycom.mooncake.FWNAT.RPAD_MP_AVC_VMR_Mixed_User
import com.polycom.mooncake.FWNAT.RPAD_MP_DMA_Dial_Out
import com.polycom.mooncake.FWNAT.RPAD_MP_MIXED_VMR_RMX
import com.polycom.mooncake.FWNAT.RPAD_P2P_Guest_H323
import com.polycom.mooncake.FWNAT.RPAD_P2P_Guest_SIP
import com.polycom.mooncake.FWNAT.RPAD_P2P_Mooncake_H323
import com.polycom.mooncake.FWNAT.RPAD_P2P_Mooncake_SIP
import com.polycom.mooncake.FWNAT.RPAD_P2P_Provision_H323
import com.polycom.mooncake.FWNAT.RPAD_P2P_Provision_SIP
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
//        RPAD_Keepalive,
//        RPAD_MP_AVC_Bridge_RMX,
//        RPAD_MP_AVC_VEQ_RMX,
//        RPAD_MP_AVC_VMR_Guest_And_Remote_User_Only_RMX,
//        RPAD_MP_AVC_VMR_Mixed_User,
//        RPAD_MP_DMA_Dial_Out,
//        RPAD_MP_MIXED_VMR_RMX,
//        RPAD_P2P_Provision_H323,
//        RPAD_P2P_Provision_SIP,
//        RPAD_P2P_Mooncake_H323,
//        RPAD_P2P_Mooncake_SIP,
//        RPAD_P2P_Guest_H323,
        RPAD_P2P_Guest_SIP
])
class FWNAT_Suite {
}
