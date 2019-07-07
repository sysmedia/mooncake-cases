package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-04-12.
 *
 * Test different dial string for MP call when SUT registered SIP
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integraded with MCU for both H.323 and SIP
 *
 * Check audio, video, content sending
 * Test with TCP and UDP
 */
@CollectSutLogIfFailed
class DialString_MP_SIP_Registered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    String vmr = "1913"

    @Shared
    String vmrWithPwd = "1914"

    @Shared
    String confPwd = "1234"

    @Shared
    String mcuConfNum = "3873"

    @Shared
    String sipUri

    def setupSpec() {
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        //Create conference on the MCU
        mcu.createConference(mcuConfNum, mcuConfNum, "", "", mcuConfProfile, "true")
        sipUri = generateDialString(moonCake).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)

        //Delete the conference on the MCU
        mcu.deleteConferenceByName(mcuConfNum)
        testContext.releaseSut(mcu)
        moonCake.registerSip("TCP", false, "", "", "", "", "")
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

        when: "Set the mooncake SIP transport protocol with registering the SIP server"
        moonCake.registerSip(sipTransProtocol, true, "", dma.ip, "", sipUri, "")

        then: "Verify if the MoonCake SIP URI is registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(sipUri)
        }

        then: "Place call on the mooncake with different call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
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
        [dialString, sipTransProtocol, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify MoonCake Can Join The Conference With Dial String #dialString And UDP SIP Transport Protocol In Call Rate #callRate Kbps"(String dialString,
                                                                                                                                          String dtmf,
                                                                                                                                          int callRate) {
        setup:
        dma.createConferenceTemplate(confTmpl, "AVC only template", String.valueOf(callRate), ConferenceCodecSupport.AVC)
        retry(times: 10, delay: 30) {
            dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)
            dma.createVmr(vmrWithPwd, confTmpl, poolOrder, dma.domain, dma.username, confPwd, null)
        }

        when: "Set the mooncake SIP transport protocol without register the SIP server"
        moonCake.registerSip("UDP", true, "", dma.ip, "", sipUri, "")

        then: "Verify if the MoonCake SIP URI is registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(sipUri)
        }

        then: "Place call on the mooncake with different call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
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
        [dialString, dtmf, callRate] << getTestData_2()
    }

    /**
     * Dial String for TCP protocol
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        callRateList.each {
            rtn << [vmr, "TCP", it]
            rtn << [vmrWithPwd + "**" + confPwd, "TCP", it]
            rtn << [mcuConfNum, "TCP", it]
            rtn << [mcuPrefix + mcuConfNum, "TCP", it]
            rtn << [vmrWithPwd + "**" + confPwd, "UDP", it]
            rtn << [entryQueue, "UDP", it]
        }
        return rtn
    }

    /**
     * Dial String for UDP protocol
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        callRateList.each {
            rtn << [vmrWithPwd, confPwd + "#", it]
            rtn << [entryQueue, mcuConfNum + "#", it]
        }
        return rtn
    }
}
