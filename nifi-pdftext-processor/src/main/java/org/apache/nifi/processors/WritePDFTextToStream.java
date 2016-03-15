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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.Integer;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.logging.ProcessorLog;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.io.InputStreamCallback;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.PDFText2HTML;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;




@SideEffectFree
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"PDF", "Text"})
@CapabilityDescription("Extract Text from a PDF File.")
public class WritePDFTextToStream extends AbstractProcessor { 

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Text Extracted")
            .build();
    public static final Relationship FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Internal Failure in Processing")
            .build();
	public static final Relationship NOTPDF = new Relationship.Builder()
            .name("notPDF")
            .description("There were no identifiable PDF Text Artifacts in the file")
            .build();
	public static final PropertyDescriptor PDFFILE = new PropertyDescriptor.Builder()
            .name("PDF File")
			.description("The file to extract text from ")
			.required(false)
			.expressionLanguageSupported(false)
			.build();


	private List<PropertyDescriptor> properties;
	private Set<Relationship> relationships;

    private String resourceData;

    @Override
    protected void init(final ProcessorInitializationContext context) {

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
	    relationships.add(NOTPDF);

        this.relationships = Collections.unmodifiableSet(relationships);

        final InputStream resourceStream = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("file.txt");
        try {
            this.resourceData = IOUtils.toString(resourceStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load resources", e);
        } finally {
            IOUtils.closeQuietly(resourceStream);
        }

    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

	@Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return this.properties;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context,
            final ProcessSession session) throws ProcessException {
        FlowFile flowfile = session.get();
        if (flowfile == null) {
              return;
        }
        final ProcessorLog log = this.getLogger();
        final AtomicReference<String> value = new AtomicReference<> ();
		final AtomicReference<String> fileName = new AtomicReference<> ();
        
		session.read(flowfile, new InputStreamCallback() {
            @Override
			OuterClass.this.fileName.set(flowfile.getAttribute("PDFFILE"));
			if(fileName.get()==null) fileName.set(context.getProperty("PDFFILE").asString());
			if(fileName==null) throw new IOException("No PDF File Specified");
            public void process(InputStream in) 
                    throws IOException 
            {
			    PDDocument doc = null;
		    	String result;
				try{
					PDFTextStripper stripper = null;
					stripper = new PDFText2HTML("UTF-8");
					stripper.setForceParsing( false );
					stripper.setSortByPosition( false );
					stripper.setShouldSeparateByBeads( true );
					stripper.setStartPage( 1 );
					stripper.setEndPage( Integer.MAX_VALUE );
					FlowFile flowfile = session.get();
					String filename = new String(flowfile.getAttribute("PDFFILE"));
					
					doc = PDDocument.load(fileName.get(), false);
                    //if( document.isEncrypted() )
                    //{
                    //    StandardDecryptionMaterial sdm = new StandardDecryptionMaterial( password );
					//	//session.getAttribute()
                    //    document.openProtection( sdm );
                    //}
					//AccessPermission ap = document.getCurrentAccessPermission();
					//if( ! ap.canExtractContent() )
					//{
					//	throw new IOException( "You do not have permission to extract text" );
					//}
					
					result = stripper.getText(doc);
					
					PDDocumentInformation info = doc.getDocumentInformation();

					value.set(result);
					// Write the results to an attribute 
					String results = value.get();
					
					if(results != null && !results.isEmpty()){
						DateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");
						flowfile = session.putAttribute(flowfile, "text", results);
						flowfile = session.putAttribute(flowfile, "page.count", Integer.toString(doc.getNumberOfPages()));
						flowfile = session.putAttribute(flowfile, "title", info.getTitle() );
						flowfile = session.putAttribute(flowfile, "author", info.getAuthor() );
						flowfile = session.putAttribute(flowfile, "subject", info.getSubject() );
						flowfile = session.putAttribute(flowfile, "keywords", info.getKeywords() );
						flowfile = session.putAttribute(flowfile, "creator", info.getCreator() );
						flowfile = session.putAttribute(flowfile, "producer", info.getProducer() );
						flowfile = session.putAttribute(flowfile, "date.creation", dformat.format(info.getCreationDate()) );
						flowfile = session.putAttribute(flowfile, "date.modified", dformat.format(info.getModificationDate()));
						flowfile = session.putAttribute(flowfile, "trapped", info.getTrapped() );
					} 
				} finally {
					if(doc!=null) doc.close();
				}
            }
		}
		);  
			
			
		// To write the results back out to flow file 
		flowfile = session.write(flowfile, new OutputStreamCallback() {

		@Override
		public void process(OutputStream out) throws IOException {
			out.write(value.get().getBytes());
		}
		});
		
			
		session.transfer(flowfile, SUCCESS);

    }
}
