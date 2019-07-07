package com.polycom.mooncake.Interop.FTP


import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec

/**
 * Created by taochen on 2019-06-14.
 *
 * Provisioning server: FTP
 * Provision items: ftp server doesn't have mac cfg file, Oculus should do reprovisioning every 10 minutes.
 */
class FTP_Provisioning_No_Mac_CFG_File extends MoonCakeProvisionTestSpec {
    def "Verify if the MoonCake will automatically open the SIP function even when the FTP does not have the config file"() {
        when: "Delete the configuration on the FTP server so that the MoonCake cannot retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(10)

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED || moonCake.sipStatus == ServiceStatus.UNKNOWN
        }
    }
}
