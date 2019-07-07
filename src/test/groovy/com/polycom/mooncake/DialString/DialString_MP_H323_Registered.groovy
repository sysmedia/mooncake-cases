package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_h323_alias_type.PlcmH323AliasType
import com.polycom.api.rest.plcm_h323_identity.H323RegistrationState
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
 * Created by taochen on 2019-04-18.
 *
 * Test different dial string when H.323 registered for MP call, standalone mode
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integraded with MCU for both H.323 and SIP
 *
 * Check audio, video, content sending
 */
@CollectSutLogIfFailed
class DialString_MP_H323_Registered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    String vmr = "1915"

    @Shared
    String vmrWithPwd = "1916"

    @Shared
    String confPwd = "1234"

    @Shared
    String mcuConfNum = "3873"

    @Shared
    String h323Name

    @Shared
    String e164Num

    def setupSpec() {
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        //Create conference on the MCU
        mcu.createConference(mcuConfNum, mcuConfNum, "", "", mcuConfProfile, "true")
        def dialString = generateDialString(moonCake)
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)

        //Delete the conference on the MCU
        mcu.deleteConferenceByName(mcuConfNum)
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

        when: "Set the mooncake with registering the GK"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")

        then: "Verify if the MoonCake is registered on the GK server"
        retry(times: 5, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == h323Name
        }

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
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")

        then: "Verify if the MoonCake is registered on the GK server"
        retry(times: 5, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == h323Name
        }

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
            rtn << [vmr, it]
            rtn << [mcuPrefix + mcuConfNum, it]
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
            rtn << [vmrWithPwd, confPwd + "#", it]
            rtn << [mcuPrefix, mcuConfNum + "#", it]
        }
        return rtn
    }
}
