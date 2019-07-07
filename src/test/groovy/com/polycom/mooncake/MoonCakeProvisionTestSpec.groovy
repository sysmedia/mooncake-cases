package com.polycom.mooncake


import com.polycom.honeycomb.MoonCake
import spock.lang.Shared

/**
 * Created by taochen on 2019-06-10.
 */
class MoonCakeProvisionTestSpec extends MoonCakeSystemTestSpec {
    @Shared
    String DHCP_SERVER

    @Shared
    String DHCP_IP_SCOPE

    @Shared
    String FTP_SERVER

    @Shared
    String FTP_USER

    @Shared
    String FTPS_USER

    @Shared
    String FTP_OPTION60_USER

    @Shared
    String FTPS_ANONYMOUS_USER

    @Shared
    String FTP_ANONYMOUS_USER

    @Shared
    String FTP_PASSWORD

    @Shared
    String FTPS_PASSWORD

    @Shared
    String cmdDel66

    @Shared
    String cmdDel160

    @Shared
    String cmdFtpsSet66

    @Shared
    String cmdFtpSet66

    @Shared
    String cmdFtpSet60

    @Shared
    String cmdDel60

    @Shared
    String cmdFtpSet66WithEmpty

    @Shared
    String cmdFtpsAnonymousSet66

    @Shared
    String cmdFtpAnonymousSet66

    @Shared
    String cmdFtpsSet160

    @Shared
    String cmdFtpSet160WithEmpty

    @Shared
    String cmdFtpSet160

    @Shared
    String cmdFtpsAnonymousSet160

    @Shared
    String cmdFtpAnonymousSet160

    @Shared
    String cmdDns

    @Shared
    String cmdCheck

    @Shared
    String mac

    def setupSpec() {
        moonCake = testContext.bookSut(MoonCake.class, "provision")

        mac = moonCake.deviceInfo.address.mac.replaceAll(":", "")
        DHCP_SERVER = testContext.getValue("DHCP_SERVER")
        DHCP_IP_SCOPE = testContext.getValue("DHCP_IP_SCOPE")
        FTP_SERVER = testContext.getValue("FTP_SERVER")
        FTP_USER = testContext.getValue("FTP_USER")
        FTPS_USER = testContext.getValue("FTPS_USER")
        FTP_ANONYMOUS_USER = testContext.getValue("FTP_ANONYMOUS_USER")
        FTPS_ANONYMOUS_USER = testContext.getValue("FTPS_ANONYMOUS_USER")
        FTP_PASSWORD = testContext.getValue("FTP_PASSWORD")
        FTPS_PASSWORD = testContext.getValue("FTPS_PASSWORD")
        FTP_OPTION60_USER = testContext.getValue("FTP_OPTION60_USER")

        cmdDel66 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE delete optionvalue 66")
        cmdDel60 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE delete optionvalue 60")
        cmdDel160 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE delete optionvalue 160")
        cmdFtpsSet66 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 66 string ftps://$FTPS_USER:$FTPS_PASSWORD@$FTP_SERVER")
        cmdFtpSet66 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 66 string ftp://$FTP_USER:$FTP_PASSWORD@$FTP_SERVER")
        cmdFtpSet60 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 60 string ftp://$FTP_OPTION60_USER:$FTP_PASSWORD@$FTP_SERVER")
        cmdFtpSet66WithEmpty = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 66 string ")
        cmdFtpsSet160 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 160 string ftps://$FTPS_USER:$FTPS_PASSWORD@$FTP_SERVER")
        cmdFtpSet160 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 160 string ftp://$FTP_USER:$FTP_PASSWORD@$FTP_SERVER")
        cmdFtpSet160WithEmpty = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 160 string ")
        cmdFtpsAnonymousSet66 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 66 string ftps://$FTPS_ANONYMOUS_USER@$FTP_SERVER")
        cmdFtpAnonymousSet66 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 66 string ftp://$FTP_ANONYMOUS_USER@$FTP_SERVER")
        cmdFtpsAnonymousSet160 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 160 string ftps://$FTPS_ANONYMOUS_USER@$FTP_SERVER")
        cmdFtpAnonymousSet160 = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 160 string ftp://$FTP_ANONYMOUS_USER@$FTP_SERVER")
        cmdDns = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE set optionvalue 006 IPADDRESS DhcpFullForce $DHCP_SERVER")
        cmdCheck = urlEncode("netsh dhcp server $DHCP_SERVER scope $DHCP_IP_SCOPE show optionvalue")
    }

    def cleanupSpec() {
        moonCake.init()
    }
}
