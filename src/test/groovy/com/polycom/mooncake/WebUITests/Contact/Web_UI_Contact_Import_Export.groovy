package com.polycom.mooncake.WebUITests.Contact

import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.ContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.DeleteAllContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.ExportContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.ImportContactPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/04/2019
 */
class Web_UI_Contact_Import_Export extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Contact_Import_Export.class)

    @Shared
    String downloadPath = "src/test/resources/WebUI/Download/directory.csv"

    @Shared
    String uploadPath = "src/test/resources/WebUI/directory.csv"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage
        deviceStatusPage.getSideBarMenu().switchContactsPage()
    }

    def "Import contact csv file test"(){
        when: "Click the delete all button"
        ContactsPage contactsPage = browser.at ContactsPage
        contactsPage.getDeleteAllContacts().click()

        and: "Click ok button"
        DeleteAllContactsPage deleteAllContactPage = browser.at DeleteAllContactsPage
        deleteAllContactPage.getOkButton().click()

        then: "Check value"
        contactsPage.getContactItems().size() == 0
        pauseTest(5)

        when:"Click import button"
        contactsPage.getImportContacts().click()
        ImportContactPage importContactPage = browser.at ImportContactPage

        and:"Upload csv file"
        importContactPage.uploadFile(uploadPath)
        importContactPage.getImportButton().click()
        pauseTest(10)
        contactsPage = browser.at ContactsPage

        then:"Check value"
        contactsPage.getContactItems().size() == 3

        cleanup:
        contactsPage = browser.at ContactsPage
        contactsPage.getDeleteAllContacts().click()
        deleteAllContactPage = browser.at DeleteAllContactsPage
        deleteAllContactPage.getOkButton().click()
    }

    def "Export contact csv file test"() {
        setup: "Remove export file if exist"
        //Bypass export part for ie
        if(System.getProperty("geb.env")=="ie") {
            return
        }
        //Remove export file first if exist
        if(new File(downloadPath).exists()){
            new File(downloadPath).delete()
        }

        when:"Export csv file"
        ContactsPage contactsPage = browser.at ContactsPage
        contactsPage.getExportContacts().click()
        ExportContactsPage exportContactsPage = browser.at ExportContactsPage
        exportContactsPage.getExportButton().click()

        then:"Check exported file is exist"
        retry(times: 3, delay: 5) {
            new File(downloadPath).exists()
        }
    }
}
