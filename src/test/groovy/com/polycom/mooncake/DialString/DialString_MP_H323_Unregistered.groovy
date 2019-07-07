package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-04-18.
 */
@CollectSutLogIfFailed
class DialString_MP_H323_Unregistered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    String vmr = "1917"

    @Shared
    String vmrWithPwd = "1918"

    @Shared
    String confPwd = "1234"

    @Shared
    String mcuConfNum = "3873"

    @Shared
    String mcuConfNumWithPwd = "3874"

    def setupSpec() {
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        //Get the MCU signaling IP address
        mcuSignalIP = mcu.signalingIPAddress
        //Create conference on the MCU
        mcu.createConference(mcuConfNum, mcuConfNum, "", "", mcuConfProfile, "true")
        mcu.createConference(mcuConfNumWithPwd, mcuConfNumWithPwd, confPwd, "", mcuConfProfile, "true")
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)

        //Delete the conference on the MCU
        mcu.deleteConferenceByName(mcuConfNum)
        mcu.deleteConferenceByName(mcuConfNumWithPwd)
        testContext.releaseSut(mcu)
        moonCake.registerGk(false, false, "", "", "", "", "")
    }

    @Unroll
    def "Verify MoonCake Can Join The H323 Conference With Dial String #dialString In Call Rate #callRate Kbps"(String dialString,
                                                                                                                int callRate) {
        setup:
        dma.createConferenceTemplate(confTmpl, "AVC only template", String.valueOf(callRate), ConferenceCodecSupport.AVC)
        retry(times: 10, delay: 30) {
            dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)
            dma.createVmr(vmrWithPwd, confTmpl, poolOrder, dma.domain, dma.username, confPwd, null)
        }

        when: "Set the mooncake without registering the GK"
        moonCake.registerGk(false, false, "", "", "", "", "")

        then: "Place call on the mooncake with different call rate"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H.323 Call with call rate " + callRate + "===============")

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
        [dialString, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify MoonCake Can Join The H323 Conference With Dial String #dialString And DTMF #dtmf In Call Rate #callRate Kbps"(String dialString,
                                                                                                                               String dtmf,
                                                                                                                               int callRate) {
        setup:
        dma.createConferenceTemplate(confTmpl, "AVC only template", String.valueOf(callRate), ConferenceCodecSupport.AVC)
        retry(times: 10, delay: 30) {
            dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)
            dma.createVmr(vmrWithPwd, confTmpl, poolOrder, dma.domain, dma.username, confPwd, null)
        }

        when: "Set the mooncake with registering the GK"
        moonCake.registerGk(false, false, "", "", "", "", "")

        then: "Place call on the mooncake with different call rate"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(2)
            moonCake.sendDTMF(dtmf)
            pauseTest(2)
            moonCake.sendDTMF(dtmf)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H.323 Call with call rate " + callRate + "===============")

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
        [dialString, dtmf, callRate] << getTestData_2()
    }

    /**
     * Dial String without DTMF
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        callRateList.each {
            rtn << [mcuSignalIP + "##" + mcuConfNum, it]
            rtn << [dma.ip + "##" + vmr, it]
            rtn << [vmr + "@" + dma.ip, it]
        }
        return rtn
    }

    /**
     * Dial String with DTMF
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        callRateList.each {
            rtn << [dma.ip + "##" + vmrWithPwd, confPwd + "#", it]
            rtn << [vmrWithPwd + "@" + dma.ip, confPwd + "#", it]
            rtn << [mcuSignalIP + "##" + mcuConfNumWithPwd, confPwd + "#", it]
            rtn << [mcuSignalIP, mcuConfNum + "#", it]
            rtn << [mcuSignalIP + "##" + entryQueue, mcuConfNum + "#", it]
        }
        return rtn
    }
}
