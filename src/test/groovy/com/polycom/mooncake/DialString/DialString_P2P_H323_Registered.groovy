package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_h323_alias_type.PlcmH323AliasType
import com.polycom.api.rest.plcm_h323_identity.H323RegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-16.
 * Test different dial string when H323 registered, standalone mode, DMA will do dial string resolve
 * Check audio, video, content sending and receiving
 * Disable DHCP
 * Test dial out to not registered EP
 * Test dial out to registered EP
 */

@CollectSutLogIfFailed
class DialString_P2P_H323_Registered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_e164Num = mcDialString.e164Number
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify H323 P2P call with registration with dial string #dialString and call rate #callRate Kbps when remote endpoint is H323 not registered"(String dialString,
                                                                                                                                                       int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake to be H323 registered"
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164Num, "", "")

        then: "Verify if the MoonCake is registered on the GK server"
        retry(times: 5, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == mc_e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == mc_h323Name
        }

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify H323 P2P call with registration with dial string #dialString and call rate #callRate Kbps when remote endpoint is H323 registered"(String dialString,
                                                                                                                                                   int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake registered and set Group Series H323 registered"
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "Verify if the MoonCake is registered on the GK server"
        retry(times: 5, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == mc_e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == mc_h323Name
        }

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, callRate] << getTestData_2()
    }

    //Create dial string to be FE_IP
    def getTestData_1() {
        def rtn = []
        callRateList.each {
            rtn << [groupSeries.ip, it]
        }
        return rtn
    }

    /**
     * Create a list of different dial string:
     * H323Alias@FE_IP, H323e164@FE_IP, H323Alias@DMA_IP
     * H323e164@DMA_IP, H323Alias@FE_FQDN, H323e164@DMA_FQDN
     * h323:H323Alias@DMA_IP, h323:H323e164@FE_FQDN
     * @return
     */

    def getTestData_2() {
        def rtn = []
        def fe_fqdn = groupSeries.hostName + "." + groupSeries.domainName
        def dma_fqdn = dma.networkSettings.physicalHostName + "." + dma.networkSettings.dnsDomain
        callRateList.each {
            rtn << [gs_h323name + "@" + groupSeries.ip, it]
            rtn << [gs_e164 + "@" + groupSeries.ip, it]
            rtn << [gs_h323name + "@" + dma.ip, it]
            rtn << [gs_e164 + "@" + dma.ip, it]
            rtn << [gs_h323name + "@" + fe_fqdn, it]
            rtn << [gs_e164 + "@" + dma_fqdn, it]
            rtn << ["h323:" + gs_h323name + "@" + dma.ip, it]
            rtn << ["h323:" + gs_e164 + "@" + fe_fqdn, it]
        }
        return rtn
    }
}

