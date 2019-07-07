package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-04-11.
 *
 * Test different dial string when SIP unregistered for  MP call, standalone mode.
 *
 * Environment: MCU unregister H.323 and SIP to DMA, DMA integraded with MCU for both H.323 and SIP
 *
 * Check audio, video, content sending
 *
 * Test with UDP and TCP
 */
@CollectSutLogIfFailed
class DialString_MP_SIP_Unregistered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    String vmr = "1911"

    @Shared
    String vmrWithPwd = "1912"

    @Shared
    String confPwd = "1234"

    def setupSpec() {
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Verify MoonCake Can Join The Conference With Dial String #dialString And #sipTransProtocol SIP Transport Protocol In Call Rate #callRate Kbps"(String dialString,
                                                                                                                                                        String sipTransProtocol,
                                                                                                                                                        int callRate) {
        setup:
        dma.createConferenceTemplate(confTmpl, "AVC only template", String.valueOf(callRate), ConferenceCodecSupport.AVC)
        retry(times: 10, delay: 30) {
            dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)
            dma.createVmr(vmrWithPwd, confTmpl, poolOrder, dma.domain, dma.username, confPwd, null)
        }

        when: "Set the mooncake SIP transport protocol without registering the SIP server"
        moonCake.registerSip(sipTransProtocol, false, "", "", "", "", "")

        then: "Place call on the mooncake with different call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        moonCake.placeCall(dialString, CallType.SIP, callRate)
        pauseTest(10)

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(15)
        //Delete conference template
        dma.deleteConferenceTemplateByName(confTmpl)
        retry(times: 5, delay: 10) {
            //Delete VMR
            if (dma.allVmrs.find { x -> x.conferenceRoomIdentifier == vmr } != null) {
                dma.deleteVmr(vmr)
            }

            if (dma.allVmrs.find { x -> x.conferenceRoomIdentifier == vmrWithPwd } != null) {
                dma.deleteVmr(vmrWithPwd)
            }
        }

        where:
        [dialString, sipTransProtocol, callRate] << getTestData()
    }

    def getTestData() {
        def rtn = []
        callRateList.each {
            rtn << [dma.ip + "##" + vmr, "TCP", it]
            rtn << [vmr + "@" + dma.ip, "TCP", it]
            rtn << [vmrWithPwd + "**" + confPwd + "@" + dma.ip, "TCP", it]
            rtn << [dma.ip + "##" + vmr, "UDP", it]
            rtn << [vmr + "@" + dma.ip, "UDP", it]
            rtn << [vmrWithPwd + "**" + confPwd + "@" + dma.ip, "UDP", it]
        }
        return rtn
    }
}
