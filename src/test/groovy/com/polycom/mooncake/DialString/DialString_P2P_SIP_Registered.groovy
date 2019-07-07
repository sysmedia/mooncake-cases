package com.polycom.mooncake.DialString

import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
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
 * Created by Dancy Li on 2019-04-15.
 * Test different dial string when SIP is registered, standaloe mode. DMA will do dial string resolve.
 * Check audio, video, content sending and receiving
 * Test dial out to not registered EP
 * Test dial out to registered EP
 * Test udp and tcp
 */
@CollectSutLogIfFailed
class DialString_P2P_SIP_Registered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String sipUri

    @Shared
    String gs_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        sipUri = generateDialString(moonCake).sipUri
        gs_sip_username = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify SIP P2P call with registration with dial string #dialString and call rate #callRate Kbps and transport #sipTransProtocol when remote endpoint is SIP not registered"(String dialString,
                                                                                                                                                                                     String sipTransProtocol,
                                                                                                                                                                                     int callRate) {

        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake to SIP registered"
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
        pauseTest(2)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify SIP P2P call with registration with dial string #dialString and call rate #callRate Kbps and transport #sipTransProtocol when remote endpoint is SIP registered"(String dialString,
                                                                                                                                                                                 String sipTransProtocol,
                                                                                                                                                                                 int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        moonCake.registerSip(sipTransProtocol, true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "Verify if the MoonCake SIP URI is registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(sipUri)
        }

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
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
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_2()
    }

    // Create dial string: IP

    def getTestData_1() {
        def rtn = []
        callRateList.each {
            rtn << [groupSeries.ip, "TCP", it]
            rtn << [groupSeries.ip, "UDP", it]
        }
        return rtn
    }

    /**
     * Create a list of different dial string:
     * SIPURI@FE_IP, SIPURI@DMA_IP, SIPURI@FE_Host
     * SIPURI@FE_FQDN
     * @return
     */
    def getTestData_2() {
        def rtn = []
        def fe_fqdn = groupSeries.hostName + "." + groupSeries.domainName
        callRateList.each {
            rtn << [gs_sip_username + "@" + groupSeries.ip, "TCP", it]
            rtn << [gs_sip_username + "@" + groupSeries.ip, "UDP", it]
            rtn << [gs_sip_username + "@" + dma.ip, "TCP", it]
            rtn << [gs_sip_username + "@" + dma.ip, "UDP", it]
            rtn << [gs_sip_username + "@" + groupSeries.hostName, "TCP", it]
            rtn << [gs_sip_username + "@" + groupSeries.hostName, "UDP", it]
            rtn << [gs_sip_username + "@" + fe_fqdn, "TCP", it]
            rtn << [gs_sip_username + "@" + fe_fqdn, "UDP", it]
        }
        return rtn
    }
}

