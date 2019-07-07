package com.polycom.mooncake.performance

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.test.performance.PerfDataProbe
import com.polycom.honeycomb.test.performance.Performance
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-05-28.
 */
@Performance(runTimes = 250)
class Performance_Sleep_Wakeup extends MoonCakeSystemTestSpec {
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
        moonCake.setEncryption("no")

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

    def "Verify if the MoonCake can sleep and wake up correctly without impacting the operations on it"() {
        setup:
        moonCake.hangUp()

        when: "Make the MoonCake register the SIP server"
        moonCake.registerSip("TCP", true, "", dma.ip, "", mc_sipUri, "")

        then: "Make the MoonCake sleep"
        moonCake.makeItSleep(2)
        pauseTest(180)

        then: "Wake up the MoonCake"
        moonCake.wakeUp()

        then: "Place a call after the wake up"
        moonCake.placeCall(vmr, CallType.SIP, 4096)
        pauseTest(20)

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }
}
