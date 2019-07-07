package com.polycom.mooncake.Interop.FTP

import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.moonCake.enums.DateFormat
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.honeycomb.moonCake.enums.TimeFormat
import com.polycom.honeycomb.moonCake.pojo.DateAndTimeSettingsResp
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-06-19.
 *
 * Verify that SUT can get NTP server from FTP provisioning.
 * 1. SUT date/time is correct when use NTP server
 * 2. SUT date/time can be changed automatically when change timezone if get time from NTP
 * 3. SUT will update date/time based on android system when NTP is Auto
 * 4. All log files date/time can be chanage automatically when change NTP setting
 */
class FTP_Provisioning_NTP extends MoonCakeProvisionTestSpec {
    @Shared
    String displayName = "Poly Group 150"

    @Shared
    String ntpServer = "http://www.pool.ntp.org/zh/"

    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_sip_username

    @Shared
    Dma dma

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)
        def mcDialString = generateDialString(moonCake)
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_sip_username = mcDialString.sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Verify if the MoonCake can be successfully provisioned by the FTPS with SIP transport protocol #mcSipProtocol"() {
        def attributesToBeSetForFtp = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'displayName'               : displayName,
                'timeServerURL'             : ntpServer,
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtp)
        pauseTest(5)

        when: "Disable the provision on the MoonCake"
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")

        then: "Update the date and time on the MoonCake"
        moonCake.updateDateAndTimeSettings("Beijing", "auto", "", DateFormat.DD_MM_YYYY, TimeFormat.TWELVE_HOUR_CLOCK)
//        captureScreenShot(moonCake)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert moonCake.registerInfo.username == displayName
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

        then: "Get the current date and time settings on the MoonCake"
        DateAndTimeSettingsResp dateAndTimeSettings = moonCake.dateAndTimeSettings
        assert dateAndTimeSettings.dateAndTimeSettings.server.timeServerAddress == ntpServer
//        captureScreenShot(moonCake)
    }
}
