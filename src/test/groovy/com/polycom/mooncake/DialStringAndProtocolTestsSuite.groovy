package com.polycom.mooncake

import com.polycom.mooncake.DialString.*
import com.polycom.mooncake.audio.You_Are_Muted
import com.polycom.mooncake.protocol.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        AudioProtocolVerificationInH323Call,
        AudioProtocolVerificationInSIPCall,
        VideoProtocolVerificationInH323Call,
        VideoProtocolVerificationInSIPCall,
        DialString_MP_SIP_Unregistered,
        DialString_MP_SIP_Registered,
        DialString_MP_H323_Registered,
        DialString_MP_H323_Unregistered,
        DialString_P2P_H323_Registered,
        DialString_P2P_H323_Unregistered,
        DialString_P2P_SIP_Registered,
        DialString_P2P_SIP_Unregistered,
        DialString_Negative_H323,
        DialString_Negative_SIP,
        SIP_Protocol_TCP_UDP_TLS_MP,
        SIP_Protocol_TCP_UDP_TLS_P2P,
        You_Are_Muted
])
class DialStringAndProtocolTestsSuite {
}
