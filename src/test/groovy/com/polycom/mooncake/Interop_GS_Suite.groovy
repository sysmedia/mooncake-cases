package com.polycom.mooncake

import com.polycom.mooncake.Interop.Endpoints.Interop_GS_Internal_MCU_H323_High_Call_Rate
import com.polycom.mooncake.Interop.Endpoints.Interop_GS_Internal_MCU_H323_Low_Call_Rate
import com.polycom.mooncake.Interop.Endpoints.Interop_GS_Internal_MCU_SIP_High_Call_Rate
import com.polycom.mooncake.Interop.Endpoints.Interop_GS_Internal_MCU_SIP_Low_Call_Rate
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_GS_H323
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_GS_H323_Unregistered
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_GS_SIP
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_GS_SIP_Unregistered
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Interop_Matrix_GS_H323,
        Interop_Matrix_GS_H323_Unregistered,
        Interop_Matrix_GS_SIP,
        Interop_Matrix_GS_SIP_Unregistered,
        Interop_GS_Internal_MCU_H323_High_Call_Rate,
        Interop_GS_Internal_MCU_H323_Low_Call_Rate,
        Interop_GS_Internal_MCU_SIP_High_Call_Rate,
        Interop_GS_Internal_MCU_SIP_Low_Call_Rate
])
class Interop_GS_Suite {
}
