package com.polycom.mooncake

import com.polycom.mooncake.performance.Performace_P2P_Short_Call_H323
import com.polycom.mooncake.performance.Performance_Idle
import com.polycom.mooncake.performance.Performance_MP_Confidence_Test_AVC
import com.polycom.mooncake.performance.Performance_MP_Long_Call_AVC
import com.polycom.mooncake.performance.Performance_P2P_Short_Call_SIP
import com.polycom.mooncake.performance.Performance_Sleep_Wakeup
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-15.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Performance_P2P_Short_Call_SIP,
        Performace_P2P_Short_Call_H323,
        Performance_MP_Long_Call_AVC,
        Performance_Idle,
        Performance_MP_Confidence_Test_AVC,
//        Performance_Sleep_Wakeup
])
class PerformanceTestsSuite {
}
