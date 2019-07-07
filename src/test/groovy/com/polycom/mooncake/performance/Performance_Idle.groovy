package com.polycom.mooncake.performance

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.performance.PerfDataProbe
import com.polycom.honeycomb.test.performance.Performance
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-05-28.
 *
 * Performance test for SUT put in idle for a long time
 * Monitor CPU, Memory, disc usage and temperature during the test
 * SUT can place call normally after put in idle for a long time
 * Test in active mode and sleep mode
 * Test when smartpairing auto detection on and sleep mode on
 */
@Performance(runTimes = 1)
class Performance_Idle extends MoonCakeSystemTestSpec {
    @Shared
    PerfDataProbe probe

    @Shared
    Dma dma

    @Shared
    String mc_sipUri

    @Shared
    String vmr = "1915"

    def setupSpec() {
        moonCake.enableSIP()
        moonCake.setEncryption("yes")

        dma = testContext.bookSut(Dma.class, keyword)

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)

        mc_sipUri = generateDialString(moonCake).sipUri
        probe = testContext.addPerfDataProbe(moonCake, 60000)
    }

    def cleanupSpec() {
        dma.deleteVmr(vmr)
        dma.deleteConferenceTemplateByName(confTmpl)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "Verify if the MoonCake can work correctly after long time idle status"() {
        setup:
        moonCake.hangUp()

        when: "Make the endpoints register the GK and SIP server"
        moonCake.registerSip("tls", true, "", dma.ip, "", mc_sipUri, "")

        then: "Make the MoonCake sleep 5 minutes and keep this status for 700 minutes"
        moonCake.makeItSleep(5)
        retry(times: 700, delay: 60) {
            assert moonCake.callStatus == "DISCONNECTED"
        }

        then: "Wake up the MoonCake"
        moonCake.wakeUp()

        then: "Make a call"
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.SIP, 4096)
        }
        pauseTest(15)

        then: "Check the media statistics on both MoonCakes"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        then: "Hang up the call"
        moonCake.hangUp()

        retry(times: 700, delay: 60) {
            assert moonCake.callStatus == "DISCONNECTED"
        }

        then: "Make a call again after 700 minutes idle"
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.SIP, 4096)
        }
        pauseTest(15)

        then: "Check the media statistics on both MoonCakes"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
    }
}
