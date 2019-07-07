package com.polycom.mooncake

import com.polycom.mooncake.Interop.Audio.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-06-02.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Interop_Audio_Mute_MP_AVC,
        Interop_Audio_Mute_MP_Mixed,
        Interop_Audio_Mute_P2P_GS,
        Interop_Audio_Mute_P2P_MoonCake,
        Interop_Audio_Mute_P2P_RPD
])
class Interop_Audio_Suite {
}
