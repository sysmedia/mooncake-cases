package com.polycom.mooncake.audio

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-05-28.
 *
 * Test You are Muted reminder will show if people start talking when the microphone is muted
 * Test unmute/mute microphone many times in the call, the feature should work well
 * Test with Standalone mode and provision mode
 * Test with H.323/SIP, AVC/SVC
 */
class You_Are_Muted extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    RpdWin rpdWin

    @Shared
    String mc_sipUri

    @Shared
    String rpd_h323Name

    @Shared
    String rpd_e164Num

    @Shared
    String rpd_sipUri

    @Shared
    String avcVmr = "1915"

    @Shared
    String mixVmr = "1916"

    @Shared
    String mixedConfTmpl = "Auto_Mixed_Conference_Template"

    def setupSpec() {
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        rpdWin.init()
        rpdWin.enableH323()
        moonCake.enableSIP()
        moonCake.setEncryption("no")

        //Create AVC only and Mixed conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)
        dma.createConferenceTemplate(mixedConfTmpl, "Mixed template", "2048", ConferenceCodecSupport.MIXED)

        //Create VMR on DMA
        dma.createVmr(avcVmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)
        dma.createVmr(mixVmr, mixedConfTmpl, poolOrder, dma.domain, dma.username, null, null)

        def rpdDialString = generateDialString(rpdWin)
        mc_sipUri = generateDialString(moonCake).sipUri
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164Num = rpdDialString.e164Number
        rpd_sipUri = rpdDialString.sipUri
    }

    def cleanupSpec() {
        dma.deleteVmr(avcVmr)
        dma.deleteVmr(mixVmr)
        dma.deleteConferenceTemplateByName(confTmpl)
        dma.deleteConferenceTemplateByName(mixedConfTmpl)
        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
        moonCake.init()
    }

    @Unroll
    def "Verify the audio mute function during the call"(String vmr) {
        when: "Register the endpoints onto the GK and SIP server"
        moonCake.registerSip("TCP", true, "", dma.ip, "", mc_sipUri, "")
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164Num)
        rpdWin.registersip(dma.ip, rpd_sipUri, "", "", "", "TCP")

        then: "Make the endpoints place call"
        moonCake.placeCall(vmr, CallType.SIP, 2048)
        rpdWin.placeCall(vmr, CallType.H323, 1920)
        pauseTest(5)

        then: "Make the MoonCake audio mute"
        moonCake.muteAudio(true)

        then: "Capture the screenshot on the MoonCake"
        captureScreenShot(moonCake)
        pauseTest(10)

        then: "Capture the screenshot on the MoonCake again to make the mute reminder disappear"
        captureScreenShot(moonCake)

        then: "Make the MoonCake audio unmute"
        moonCake.muteAudio(false)

        then: "Capture the screenshot on the MoonCake to make sure there is not the mute reminder"
        captureScreenShot(moonCake)

        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(2)

        where:
        vmr    | _
        avcVmr | _
        mixVmr | _
    }
}
