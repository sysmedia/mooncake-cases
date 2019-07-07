package com.polycom.mooncake.FWNAT

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_dial_out_participant.PlcmDialOutParticipant
import com.polycom.honeycomb.dma.pojofactory.DmaApiSharedPojoFactory
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by dyuan on 6/5/2019
 *
 * # DMA dial out VMR, external Oculus H323/SIP regsitered
 * # 1. H323 registered users
 * # 2. SIP registered users
 * # RPAD environment
 */
class RPAD_MP_DMA_Dial_Out extends MoonCakeSystemTestSpec {
    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String vmr = "8888"

    @Shared
    String confPwd = "1234"

    @Shared
    def mc_alias

    @Shared
    def rpd_alias

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")

        dma = testContext.bookSut(Dma.class, "FWNAT")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        mc_alias = generateDialString(moonCake)
        rpd_alias = generateDialString(rpdWin)

        rpdWin = testContext.bookSut(RpdWin.class, "FWNAT")
        rpdWin.init()
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
    }

    def "Verify Internal RPD join VMR with dial out external mooncake via SIP"() {
        setup:
        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpd_alias.sipUri, "polycom.com", "", "", "TLS")

        moonCake.enableSIP()
        moonCake.setEncryption("auto")
        moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")

        PlcmDialOutParticipant partMc = new PlcmDialOutParticipant();
        partMc.setName("mooncake");
        partMc.setDialString("sip:" + mc_alias.sipUri + "@polycom.com");

        dma.createVmr(vmr, confTmpl, "Factory Pool Order", dma.domain, dma.username, confPwd, null, partMc)

        when: "Rpdwin call in"
        rpdWin.placeCall(vmr, CallType.SIP, 2048)
        pauseTest(2)
        rpdWin.sendDtmf(confPwd + "#")
        pauseTest(15)


        then: "Verify the rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        cleanup:
        rpdWin.hangUp()
        moonCake.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(5)
    }

    def "Verify Internal RPD join VMR with dial out external mooncake via H323"() {
        setup:
        rpdWin.enableH323()
        rpdWin.registerH323(dma.ip, rpd_alias.h323Name, rpd_alias.e164Number)

        moonCake.enableH323()
        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")

        PlcmDialOutParticipant partMc = new PlcmDialOutParticipant();
        partMc.setName("mooncake");
        partMc.setDialString("h323:" + mc_alias.h323Name);

        dma.createVmr(vmr, confTmpl, "Factory Pool Order", dma.domain, dma.username, confPwd, null, partMc)

        when: "Rpdwin call in"
        rpdWin.placeCall(vmr, CallType.H323, 2048)
        pauseTest(2)
        rpdWin.sendDtmf(confPwd + "#")
        pauseTest(15)


        then: "Verify the rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        cleanup:
        rpdWin.hangUp()
        moonCake.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(5)
    }
}
