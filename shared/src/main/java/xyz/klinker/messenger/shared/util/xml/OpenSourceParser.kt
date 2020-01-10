/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util.xml

import android.content.Context
import android.content.res.XmlResourceParser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.util.ArrayList

import xyz.klinker.messenger.shared.R

/**
 * Helper for parsing the open_source.xml resource into a usable array of strings that can
 * be displayed in an adapter.
 */
object OpenSourceParser {

    private val ns: String? = null

    /**
     * Parses the open_source.xml file and returns a string array of each item.
     *
     * @param context the current application context.
     * @return an array of strings for formatting and displaying.
     */
    fun parse(context: Context): Array<String>? = try {
        val parser = context.resources.getXml(R.xml.open_source)
        parser.next()
        parser.nextTag()
        readChangelog(parser)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readChangelog(parser: XmlPullParser): Array<String> {
        val items = ArrayList<String>()

        parser.require(XmlPullParser.START_TAG, ns, "open_source")
        while (parser.next() != XmlPullParser.END_TAG) {
            val name = parser.name
            if ("item" == name) {
                items.add(readVersion(parser))
            }
        }

        return items.toTypedArray()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersion(parser: XmlPullParser): String {
        val versionInfo = StringBuilder()
        parser.require(XmlPullParser.START_TAG, ns, "item")
        versionInfo.append(readVersionNumber(parser))

        while (parser.next() != XmlPullParser.END_TAG) {
            val name = parser.name
            if ("license" == name) {
                versionInfo.append(readVersionText(parser))
            }
        }

        return versionInfo.toString()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersionNumber(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "item")
        var name = parser.getAttributeValue(null, "name")
        val owner = parser.getAttributeValue(null, "owner")
        name = "<b><u>$name:</b></u>"
        if (owner != null) {
            name += " " + owner
        }
        name += "<br/><br/>"
        return name
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersionText(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "license")
        val text = readText(parser).replace("    ", "").trim { it <= ' ' }
        parser.require(XmlPullParser.END_TAG, ns, "license")
        return text
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
}