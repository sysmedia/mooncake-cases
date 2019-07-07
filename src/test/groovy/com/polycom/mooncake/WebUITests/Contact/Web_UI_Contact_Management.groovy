package com.polycom.mooncake.WebUITests.Contact

import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.ContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.DeleteAllContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.DeleteContactPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.EditContactPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/04/2019
 */
class Web_UI_Contact_Management extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Contact_Management.class)

    @Shared
    String filePath = "src/test/resources/WebUI/testChars.cfg"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage
        ContactsPage contactsPage = deviceStatusPage.getSideBarMenu().switchContactsPage()
    }

    def "Manage contacts test"() {
        setup:
        def contacts = convertCsvFileToMap(filePath,"\t")

        when:"Open Contact Page"
        ContactsPage contactsPage = browser.at ContactsPage

        and:"Delete all existing contacts"
        contactsPage.deleteAllContacts.click()
        DeleteContactPage deleteContactPage = browser.at DeleteContactPage
        deleteContactPage.getOkButton().click()

        then:"Check value"
        contactsPage.getContactItems().size() == 0


        when: "Input contact name and number"
        contactsPage = browser.at ContactsPage
        contacts.each{
            name,number->
                contactsPage = browser.at ContactsPage
                contactsPage.getAddContact().click()
                EditContactPage editContactPage = browser.at EditContactPage
                editContactPage.inputName(name)
                editContactPage.inputNumber(number)
                editContactPage.saveButton.click()
                pauseTest(5)
        }

        then:"Check numbers"
        contactsPage.getNumberOfContacts() == 61

        when:"Go to the last page"
        contactsPage.goToPageNumber.value("7")
        contactsPage.goToPage.click()
        pauseTest(5)
        contactsPage = browser.at ContactsPage

        and:"Click previous page"
        contactsPage.previousPage.click()

        and:"Click next page"
        contactsPage.nextPage.click()
        contactsPage = browser.at ContactsPage

        then:"Check contact number in last page"
        contactsPage.getContactItems().size() == 1

        when: "Delete the contact number in first line"
        contactsPage.getContactItems().get(0).getDeleteButton().click()

        and: "Click ok button"
        deleteContactPage = browser.at DeleteContactPage
        deleteContactPage.getOkButton().click()
        contactsPage = browser.at ContactsPage

        then:"Check value"
        contactsPage.getNumberOfContacts() == 60

        cleanup:
        contactsPage = browser.at ContactsPage
        contactsPage.getDeleteAllContacts().click()
        DeleteAllContactsPage deleteAllContactPage = browser.at DeleteAllContactsPage
        deleteAllContactPage.getOkButton().click()
    }
}
