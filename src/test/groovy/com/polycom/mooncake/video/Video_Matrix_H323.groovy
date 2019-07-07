package com.polycom.mooncake.video

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created  on 2019-05-16.
 */



class Video_Matrix_H323 extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

     @Shared
    def gs_h323name = "Auto_GS550"

    @Shared
    def gs_e164 = "1721126550"

    @Shared
    String h323Name = "automooncake256"

    @Shared
    String e164Num = "84381256"


    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, "GS550")
        groupSeries.init()
        groupSeries.setEncryption("no")

        dma = testContext.bookSut(Dma.class, keyword)

        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        testContext.releaseSut(groupSeries)
    }

    @Unroll
    def "Verify Video Protocol In H323 Call "(int callRate, String videoProtocolRx, String videoProtocalTx,
                                              String formatRx, String formatTx
          ) {
        setup:
        moonCake.hangUp()

        when: "Set the MoonCake Call Setting"
        moonCake.updateCallSettings(callRate, "off", true, false, true);

        then: "MoonCake place call to the Group with various call rate and then verify the media statistics"

        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_e164, CallType.H323, callRate)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${videoProtocolRx}:${formatRx}:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${videoProtocalTx}:${formatTx}:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [callRate, videoProtocolRx, videoProtocalTx, formatRx, formatTx] << getTestData()
    }

    def getTestData() {
        def rtn = []

        rtn << [256, "H.264High", "H.264High", "640x368", "640x360"]

        return rtn
    }
}
