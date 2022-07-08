/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jml;

import net.sf.jml.message.p2p.DisplayPictureRetrieveWorker;

/**
 * This interface is used to be notified about the progress of the display 
 * picture (also emoticons) retrieval.
 * 
 * @author Angel Barragán Chacón
 */
public interface DisplayPictureListener {

	/**
	 * Types of finalization for the MsnObject request process.
	 */
	public enum ResultStatus { 
		
		/**
		 * MsnObject has been retrieved successfuly.
		 */
		GOOD, 
		
		/**
		 * There has been a file access error.
		 */
		FILE_ACCESS_ERROR, 
		
		/**
		 * There has been a protocol error.
		 */
		PROTOCOL_ERROR, 
		
		/**
		 * Thre has been an unknown error.
		 */
		UNKNOWN }
	

	/**
	 * Notify about the MsnObject finalization retrieval.
	 * 
	 * @param messenger Instance of the messenger we are working with. 
	 * @param worker MsnObject retrieved.
	 * @param msnObject Instance of the MsnObject retrieved.
	 * @param result Type of result for the process.
     * @param resultBytes Bytes of data returned.
	 * @param context Context for the result. For FILE_ACCESS_ERROR and INKNOWN, 
	 * it is the exception instance for the error. For PROTOCOL_ERROR, it is an
	 * instance of Integer with the P2P flag field value.
	 */
	public void notifyMsnObjectRetrieval(MsnMessenger messenger,
			                             DisplayPictureRetrieveWorker worker,
			                             MsnObject msnObject,
			                             ResultStatus result,
                                         byte[] resultBytes,
			                             Object context);

}
