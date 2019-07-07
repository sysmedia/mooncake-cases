package com.polycom.mooncake.provision

import com.polycom.api.rest.plcm_h323_alias_type.PlcmH323AliasType
import com.polycom.api.rest.plcm_h323_identity.H323RegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-06-12.
 *
 * Provisioning mode: DHCP auto_66
 * Provisioning profile:
 * <ProvDHCPOpt>60</ProvDHCPOpt>
 * Provisioning server: ftp
 * MoonCake will take new DHCP option from provisioning profile to do DHCP provision after reboot.
 */
class Auto_Provisioning_Server_Option_66_To_Custom_Reboot extends MoonCakeProvisionTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_e164Num

    @Shared
    String gs_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_h323Name

    @Shared
    String gs_sip_username

    @Shared
    String mc_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_e164Num = gsDialString.e164Number
        gs_h323Name = gsDialString.h323Name
        gs_sip_username = gsDialString.sipUri
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_sip_username = mcDialString.sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    def "Verify if the MoonCake can be provisioned by the DHCP option 66"() {
        def attributesToBeSet = [provision: [
                'profileUpdateCheckInterval': 'PT60S',
                'ProvDHCPOpt'               : '60',
                'enableSIP'                 : 'disable',
                'e164Number'                : mc_e164Num,
                'h323alias'                 : mc_h323Name,
                'gatekeeperAddress'         : dma.ip,
                'h323AuthEnabled'           : 'false',
                'enableH323'                : 'enable']
        ]

        String newe164Num = generateDialString(moonCake).e164Number
        def attributesToBeSet60 = [provision: [
                'profileUpdateCheckInterval': 'PT60S',
                'ProvDHCPOpt'               : '60',
                'enableSIP'                 : 'disable',
                'e164Number'                : newe164Num,
                'h323alias'                 : mc_h323Name,
                'gatekeeperAddress'         : dma.ip,
                'h323AuthEnabled'           : 'false',
                'enableH323'                : 'enable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        FtpClient ftpClient60 = new FtpClient(FTP_SERVER, FTP_OPTION60_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)
        deleteConfigOnFtp(ftpClient60, mac)
        createConfigOnFtp(ftpClient60, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel60)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet66)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet60)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSet)
        modifyConfigOnFtp(ftpClient60, mac, attributesToBeSet60)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.AUTO_CUSTOMER, 66, "", "", "", "")

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(groupSeries, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        then: "Hang up the call and reboot the MoonCake for setting from option 60"
        moonCake.hangUp()
        moonCake.reboot()
        pauseTest(60)

        then: "Verify if the settings from DHCP option 60 has been retrieved by the MoonCake"
        retry(times: 3, delay: 60) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == newe164Num
        }

        then: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(groupSeries, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        doCommandOnHttp(DHCP_SERVER, cmdDel60)
    }
}
