package com.polycom.mooncake.Interop.DMA

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Gary Wang on 2019-05-29.
 *
 * This case is to verify MoonCake calling MCU via VMR with call rate 1024K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 1024K + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class Interop_DMA_GK_Authentication extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Dma dma1

    @Shared
    def gs_h323name

    @Shared
    def gs_e164

    @Shared
    String h323Name1

    @Shared
    String e164Num1

    @Shared
    String authName1 = "mooncake1"

    @Shared
    String authName2 = "mooncake2"

    @Shared
    String authPasswd = "polycom"


    @Shared
    int callRate = 2048

    @Shared
    GroupSeries groupSeries


    def setupSpec() {
        moonCake.updateCallSettings(callRate, "off", true, false, true);
        groupSeries = testContext.bookSut(GroupSeries.class, "GS550")
        groupSeries.init()
        groupSeries.setEncryption("no")

        dma = testContext.bookSut(Dma.class, "Auth")
        dma1 = testContext.bookSut(Dma.class, keyword)
        def dialString = generateDialString(moonCake)
        h323Name1 = dialString.h323Name
        e164Num1 = dialString.e164Number

        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number

        moonCake.enableH323()
        groupSeries.enableH323()


    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        testContext.releaseSut(dma1)
        testContext.releaseSut(groupSeries)

    }

    def "Verify MoonCake H323 GK authentication and DMA enable GK authentication too "() {
        setup:

        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerGk(true, true, dma.ip, h323Name1, e164Num1, authName1, authPasswd)
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip, true, authName2, authPasswd)
        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(e164Num1, CallType.H323, callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

    }

    def "Verify MoonCake H323 GK authentication but DMA not enable "() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerGk(true, true, dma1.ip, h323Name1, e164Num1, authName1, authPasswd)
        groupSeries.registerGk(gs_h323name, gs_e164, dma1.ip)

        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(e164Num1, CallType.H323, callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

    }


}
