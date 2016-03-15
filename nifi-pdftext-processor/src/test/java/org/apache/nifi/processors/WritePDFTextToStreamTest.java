/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import java.io.*;
import java.util.*;
import java.net.URL;

public class WritePDFTextToStreamTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(WritePDFTextToStream.class);
    }

    @Test
    public void testPDFTextRead() {
		FileInputStream fstream = null;
		MockFlowFile flowFile = null;
		try {
			verifyTestRunnerFlow("/sample3.pdf", WritePDFTextToStream.SUCCESS, null);
		} catch (IOException e) {
			fail(e.getMessage());
		}
        Map<String, String> attributes = flowFile.getAttributes();
		assertEquals("1750", attributes.get("title"));
    }
	
	public MockFlowFile verifyTestRunnerFlow(String pathStr,Relationship rel, String max) throws IOException {
        URL url = this.getClass().getResource(pathStr);
		FileInputStream fstream = new FileInputStream(url.getFile());
		int inpSize = fstream.available();
		byte []b = new byte[inpSize];
		
		fstream.read(b);
		System.out.println(new String(b).toString());
		testRunner.enqueue(new String(b).toString());

        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(rel, 1);


        MockFlowFile flowFile = testRunner.getFlowFilesForRelationship(rel).get(0);
        testRunner.assertQueueEmpty();

        testRunner.enqueue(flowFile);
        testRunner.clearTransferState();
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(rel, 1);

        flowFile = testRunner.getFlowFilesForRelationship(rel).get(0);
        return  flowFile;
    }

}
