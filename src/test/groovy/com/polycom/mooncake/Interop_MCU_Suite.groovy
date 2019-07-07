package com.polycom.mooncake


import com.polycom.mooncake.Interop.MCU.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        AVC_64K_SIP,
        AVC_64K_H323,
        AVC_256K_RMX1800,
        AVC_384K_RMX4000,
        AVC_512K_RMX4000,
        AVC_768K_RMX1800,
        AVC_1024K_720p_RMX4000,
        AVC_2048K_1080p_RMX1800,
        AVC_3072K_1080p_RMX1800,
        AVC_4096K_1080p_RMX1800,
        AVC_1024K_720p_RMXVE,
        AVC_1024K_VSW,
        AVC_4096K_1080p_RMXVE,
        AVC_VEQ_AVC_VMR,
        AVC_IVR_Different_Call_Rate,
        AVC_MP_Bridge_Direct_Call_NonAES,
        Mute_Video_AVC_Encrypted_Call,
        Mute_Video_AVC_Unencrypted_Call,
        AVC_VEQ_Mixed_VMR,
        Mixed_Join_With_Content_Sending,
        Mixed_Join_When_Content_Sharing,
        AVC_1024K_720p_NGB,
        AVC_4096K_1080p_NGB,
        Mixed_2048K_NGB,
        Mixed_2048K_RMXVE,
        Mute_Video_Mixed_Encrypted_Call,
        Mute_Video_Mixed_Unencrypted_Call,
        AVC_Join_When_Content_Sharing,
        AVC_Join_With_Content_Sending,
        SVC_AVC_Mixed_Mode_1920k_720p_H323,
        SVC_AVC_Mixed_Mode_1920k_720p_SIP,
        SVC_AVC_Mixed_Mode_4096k_1080p_H323,
        SVC_AVC_Mixed_Mode_4096k_1080p_SIP,
        AVC_VEQ_VMR_MixedEncryption
])
class Interop_MCU_Suite {
}
