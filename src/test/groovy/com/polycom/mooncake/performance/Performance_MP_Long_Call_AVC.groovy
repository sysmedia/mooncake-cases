package com.polycom.mooncake.performance

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.performance.PerfDataProbe
import com.polycom.honeycomb.test.performance.Performance
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-05-28.
 *
 * Performance test for AVC MP long call for H.323 and SIP
 * SUT1 place H.323 call with highest Tx and Rx video/content resolution, and with encryption on
 * SUT2 place SIP call with highest Tx and Rx video/content resolution, and with encryption on
 * Monitor CPU, Memory and temperature during the test
 * Test AVC call without content for a long time
 * Test AVC call with content sending for a long time
 * Test AVC call with content receiving for a long time
 * Test content war for many times
 */
@Performance(runTimes = 1)
class Performance_MP_Long_Call_AVC extends MoonCakeSystemTestSpec {
    @Shared
    PerfDataProbe probe

    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    MoonCake moonCake2

    @Shared
    String mc_sipUri

    @Shared
    String gs_h323Name

    @Shared
    String gs_e164Num

    @Shared
    String mc2_h323Name

    @Shared
    String mc2_e164Num

    @Shared
    String rpd_h323Name

    @Shared
    String rpd_e164Num

    @Shared
    String vmr = "1915"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        moonCake2 = testContext.bookSut(MoonCake.class, keyword, "backup")

        groupSeries.init()
        rpdWin.init()
        moonCake2.init()

        groupSeries.setEncryption("yes")
        groupSeries.enableH323()
        rpdWin.enableH323()
        moonCake.enableSIP()
        moonCake.setEncryption("yes")
        moonCake2.enableH323()
        moonCake2.setEncryption("yes")

        dma = testContext.bookSut(Dma.class, keyword)

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)

        def gsDialString = generateDialString(groupSeries)
        def rpdDialString = generateDialString(rpdWin)
        def mc2DialString = generateDialString(moonCake2)
        mc_sipUri = generateDialString(moonCake).sipUri
        gs_h323Name = gsDialString.h323Name
        gs_e164Num = gsDialString.e164Number
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164Num = rpdDialString.e164Number
        mc2_h323Name = mc2DialString.h323Name
        mc2_e164Num = mc2DialString.e164Number

        probe = testContext.addPerfDataProbe(moonCake, 60000)
    }

    def cleanupSpec() {
        dma.deleteVmr(vmr)
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
        testContext.releaseSut(moonCake2)
        moonCake.init()
        moonCake2.init()
    }

    def "AVC MP long call for H323 and SIP"() {
        setup:
        moonCake.hangUp()
        moonCake2.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()

        when: "Make the endpoints register the GK and SIP server"
        moonCake.registerSip("tls", true, "", dma.ip, "", mc_sipUri, "")
        moonCake2.registerGk(true, false, dma.ip, mc2_h323Name, mc2_e164Num, "", "")
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164Num)

        then: "Make the endpoints please call to join the conference"
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.SIP, 4096)
            moonCake2.placeCall(vmr, CallType.H323, 4096)
            groupSeries.placeCall(vmr, CallType.H323, 4096)
            rpdWin.placeCall(vmr, CallType.H323, 1920)
        }
        pauseTest(15)

        then: "Check the media statistics on both MoonCakes"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)

        then: "Make the call hold on for 5 hours"
        retry(times: 300, delay: 60) {
            assert moonCake.callStatus == "CONNECTED"
            assert moonCake2.callStatus == "CONNECTED"
        }

        then: "Make the second MoonCake send content to the peers and continue the sending content status for 5 hours"
        moonCake2.pushContent()
        pauseTest(10)
        retry(times: 300, delay: 60) {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)
        }

        then: "Make the second MoonCake stop the content sending"
        moonCake2.stopContent()

        then: "Make the first MoonCake send content to the peers and continue the sending content status for 5 hours"
        moonCake.pushContent()
        pauseTest(10)
        retry(times: 300, delay: 60) {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)
        }

        then: "Make the first MoonCake stop the content sending"
        moonCake.stopContent()

        then: "Content war for many times"
        1.upto(200) {
            moonCake.pushContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)
            logger.info("=====MoonCake NO.1 finished content sending in round" + it + "=====")

            moonCake2.pushContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)
            logger.info("=====MoonCake NO.2 finished content sending in round" + it + "=====")
        }

        cleanup:
        moonCake.hangUp()
        moonCake2.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        pauseTest(5)
    }
}
