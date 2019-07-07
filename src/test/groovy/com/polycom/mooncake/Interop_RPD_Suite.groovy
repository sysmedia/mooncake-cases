package com.polycom.mooncake

import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Mac_H323
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Mac_SIP
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Win_H323
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Win_H323_Unregistered
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Win_SIP
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_RPD_Win_SIP_Unregistered
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Interop_Matrix_RPD_Win_H323,
        Interop_Matrix_RPD_Win_H323_Unregistered,
        Interop_Matrix_RPD_Win_SIP,
        Interop_Matrix_RPD_Win_SIP_Unregistered,
        Interop_Matrix_RPD_Mac_H323,
        Interop_Matrix_RPD_Mac_SIP
])
class Interop_RPD_Suite {
}
