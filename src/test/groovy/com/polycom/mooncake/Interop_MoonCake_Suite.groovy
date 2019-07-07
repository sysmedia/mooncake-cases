package com.polycom.mooncake

import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_MoonCake_H323
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_MoonCake_SIP
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrix_Mooncake_SIP_Unregistered
import com.polycom.mooncake.Interop.Endpoints.Interop_Matrx_MoonCake_H323_Unregistered
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-23.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Interop_Matrix_MoonCake_H323,
        Interop_Matrx_MoonCake_H323_Unregistered,
        Interop_Matrix_MoonCake_SIP,
        Interop_Matrix_Mooncake_SIP_Unregistered
])
class Interop_MoonCake_Suite {
}
