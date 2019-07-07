package com.polycom.mooncake

import com.polycom.mooncake.Interop.DMA.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-05-24.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Interop_DMA_Gateway_Call,
        Interop_DMA_Dial_Out,
        Interop_DMA_GK_Authentication
])
class Interop_DMA_Suite {
}
