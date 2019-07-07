package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 5/10/2019
 */
class AVC_IVR_Different_Call_Rate extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    String h323Name

    @Shared
    String e164Num

    @Shared
    String moonsipUri

    @Shared
    String callTmplAVC = "auto_avc_only_tmpl"

    @Shared
    String vmrAVC = "110022"

    @Shared
    String vmrAVCPwd = "1234"

    @Shared
    String veq = "660011"

    @Shared
    String avcEqName = "automation_CP_Only_EQ"

    @Shared
    def h323CallRate = [4096, 2048, 1024, 512]

    @Shared
    def sipCallRate = [3072, 1536, 768, 384, 64] //64: no this call rate for moonCake, so drop this item

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)
        dma.createConferenceTemplate(callTmplAVC, "AVC only call template", "2048", ConferenceCodecSupport.AVC)
        dma.createVmr(vmrAVC, callTmplAVC, poolOrder, dma.domain, dma.username, vmrAVCPwd, null)
        dma.createVeq(veq, avcEqName)
        def dialString = generateDialString(moonCake)
        moonsipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number

    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(callTmplAVC)
        dma.deleteVmr(vmrAVC)
        dma.deleteVeq(veq)
        testContext.releaseSut(dma)
    }

    @Unroll
    def "SUT dial in AVC conference with call rate of #callRate for H.323"(int callRate) {
        setup: "register MoonCake to H323"
        moonCake.updateCallSettings(callRate, "off", true, false, true)
        moonCake.enableH323()
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        pauseTest(5)

        when: "MoonCake place 323 call"
        logger.info("========Start place 323 call to conference with rate" + callRate + "========")
        moonCake.placeCall(vmrAVC, CallType.H323, callRate)
        pauseTest(2)
        moonCake.sendDTMF(vmrAVCPwd + "#")
        pauseTest(2)
        moonCake.sendDTMF(vmrAVCPwd + "#")
        pauseTest(2)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "MoonCake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        cleanup: "hang up call"
        moonCake.hangUp()
        logger.info("========Call end up========")

        where:
        [callRate] << getCallRate(h323CallRate)
    }

    @Unroll
    def "SUT dial in AVC VEQ - AVC VMR - VMR Password with call rate of #callRate for SIP"(int callRate) {
        setup: "register MoonCake to SIP"
        moonCake.updateCallSettings(callRate, "off", true, false, true)
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, moonsipUri, "", "")
        pauseTest(5)

        when: "MoonCake place SIP call"
        logger.info("========Start place SIP call to conference with rate" + callRate + "========")
        moonCake.placeCall(veq, CallType.SIP, callRate)
        pauseTest(3)
        moonCake.sendDTMF(vmrAVC + "#")
        pauseTest(2)
        moonCake.sendDTMF(vmrAVC + "#")
        pauseTest(2)
        moonCake.sendDTMF(vmrAVCPwd + "#")
        pauseTest(2)
        moonCake.sendDTMF(vmrAVCPwd + "#")
        pauseTest(2)

        then: "Verify the media statistics during the call"
        if (callRate == 64) {
            verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        } else {
            verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        }

        then: "MoonCake push content"
        if (callRate != 64) {
            retry(times: 3, delay: 5) {
                moonCake.pushContent()
            }
            pauseTest(5)
        }

        then: "Verify the media statistics during the call"
        if (callRate != 64) {
            verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")
        } else {
            verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        }

        cleanup: "hang up call"
        moonCake.hangUp()
        logger.info("========Call end up========")

        where:
        [callRate] << getCallRate(sipCallRate)
    }

    def getCallRate(List callRate) {
        def rtn = []
        callRate.each { rtn << [it] }
        return rtn
    }
}
