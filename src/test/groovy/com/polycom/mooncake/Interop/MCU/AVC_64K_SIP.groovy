package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_conference_template_v7.EncryptionPolicy
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Gary Wang on 2019-05-14.
 * This case is to verify MoonCake calling MCU by SIP while call rate is 64k
 * Conference profile: Line rate 1024k + Encryption auto
 */
class AVC_64K_SIP extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Mcu mcu
    
    @Shared
    String mcuConfProfileOn = "encryptionOn"

    @Shared
    String mcuConfProfileAuto = "encryptionAuto"

    @Shared
    String confPwd = "1234"

    @Shared
    String mcuConfNum = "3873"

    @Shared
    String vmr = "6666"

    @Shared
    String moonsipUri = "mooncake"
    
    def setupSpec() {
        moonCake.enableSIP()
        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        moonsipUri = dialString.sipUri
        moonCake.registerSip("TLS", true, "", dma.ip, moonsipUri, moonsipUri, "")

        try {
            dma.deleteVmr(vmrAVCAuto)
        } catch (Exception e) {
            logger.info("delete vmr auto error")
        }
        try {
            dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        } catch (Exception e) {
            logger.info("delete template auto error")
        }

        dma.createConferenceTemplate(callTmplAVCAuto, "AVC only call template AES Auto", "1024", ConferenceCodecSupport.AVC, EncryptionPolicy.ENCRYPTED_PREFERRED)
        dma.createVmr(vmrAVCAuto, callTmplAVCAuto, poolOrder, dma.domain, dma.username, null, null)
    }

    def cleanupSpec() {
        dma.deleteVmr(vmrAVCAuto)
        dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)

    }

    def "Verify MoonCake Can Join The MCU Conference With Encryption On In Call Rate 64 Kbps"(String dialString, int callRate) {
        setup:"Hangup MoonCake"
        moonCake.hangUp()

        when: "Create the MCU conference"
        moonCake.updateCallSettings(64, "on", true, false, true)



        then: "Place call on the MoonCake with encryption on and call rate 64Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP,callRate)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")

        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_1()
        
    }

    def "Verify MoonCake Can Join The MCU Conference With Encryption Off In Call Rate 64 Kbps"(String dialString,
                                                                                              int callRate) {
        setup:"Hangup MoonCake"
        moonCake.hangUp()

        when:"Set the MoonCake with encryption off"
        moonCake.updateCallSettings(64, "off", true, false, true)

        then: "Place call on the mooncake with encryption on and call rate 64K"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
        }
        
        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")

        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(10)
  
        where:
        [dialString, callRate] << getTestData_1()
    }

    def "Verify MoonCake Can Join The SIP Conference With Encryption Auto And DTMF #dtmf In Call Rate 64 Kbps"(String dialString,
                                                                                                                               String dtmf,
                                                                                                                               int callRate) {


        setup:"Set the encryption mode to auto on MoonCake"
        moonCake.hangUp()
        moonCake.updateCallSettings(64, "auto", true, false, true)

        when:"Create conference on the MCU"
        mcu.createConference(mcuConfNum, mcuConfNum, confPwd, "", mcuConfProfileAuto, "true")
        pauseTest(2)

        then: "Place call on the mooncake with call rate of 64Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(2)
            moonCake.sendDTMF(dtmf)
            pauseTest(2)
            moonCake.sendDTMF(dtmf)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")

        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        //Delete the conference on the MCU
        mcu.deleteConferenceByName(mcuConfNum)
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(3)

        where:
        [dialString, dtmf, callRate] << getTestData_2()
    }
    /**
     * Dial String with VMR
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        rtn << [vmrAVCAuto, 64]
        return rtn
    }

    /**
     * Dial String with DTMF
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [mcuPrefix + mcuConfNum,confPwd + "#",64]
        return rtn
    }
}
