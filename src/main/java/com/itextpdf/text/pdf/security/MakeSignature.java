/*
 * $Id: MakeSignature.java 5338 2012-08-18 15:40:25Z psoares33 $
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2012 1T3XT BVBA
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY 1T3XT,
 * 1T3XT DISCLAIMS THE WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.text.pdf.security;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfString;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

/**
 * Class that signs your PDF.
 * @author Paulo Soares
 */
public class MakeSignature {

	/** The Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MakeSignature.class);

    public enum CryptoStandard {
    	CMS, CADES
    }

    /**
     * Signs the document using the detached mode, CMS or CAdES equivalent.
     * @param sap the PdfSignatureAppearance
     * @param externalSignature the interface providing the actual signing
     * @param chain the certificate chain
     * @param crlList the CRL list
     * @param ocspClient the OCSP client
     * @param tsaClient the Timestamp client
     * @param externalDigest an implementation that provides the digest
     * @param estimatedSize the reserved size for the signature. It will be estimated if 0
     * @param sigtype Either Signature.CMS or Signature.CADES
     * @throws DocumentException 
     * @throws IOException 
     * @throws GeneralSecurityException 
     * @throws NoSuchAlgorithmException 
     * @throws Exception 
     */
    public static void signDetached(PdfSignatureAppearance sap, ExternalDigest externalDigest, ExternalSignature externalSignature, Certificate[] chain, Collection<CrlClient> crlList, OcspClient ocspClient,
            TSAClient tsaClient, int estimatedSize, CryptoStandard sigtype) throws IOException, DocumentException, GeneralSecurityException {
        Collection<byte[]> crlBytes = null;
        int i = 0;
        while (crlBytes == null && i < chain.length)
        	crlBytes = processCrl(chain[i++], crlList);
    	if (estimatedSize == 0) {
            estimatedSize = 8192;
            if (crlBytes != null) {
                for (byte[] element : crlBytes) {
                    estimatedSize += element.length + 10;
                }
            }
            if (ocspClient != null)
                estimatedSize += 4192;
            if (tsaClient != null)
                estimatedSize += 4192;
        }
        sap.setCertificate(chain[0]);
        PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, sigtype == CryptoStandard.CADES ? PdfName.ETSI_CADES_DETACHED : PdfName.ADBE_PKCS7_DETACHED);
        dic.setReason(sap.getReason());
        dic.setLocation(sap.getLocation());
        dic.setContact(sap.getContact());
        dic.setDate(new PdfDate(sap.getSignDate())); // time-stamp will over-rule this
        sap.setCryptoDictionary(dic);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(estimatedSize * 2 + 2));
        sap.preClose(exc);

        String hashAlgorithm = externalSignature.getHashAlgorithm();
        PdfPKCS7 sgn = new PdfPKCS7(null, chain, hashAlgorithm, null, externalDigest, false);
        InputStream data = sap.getRangeStream();
        byte hash[] = DigestAlgorithms.digest(data, externalDigest.getMessageDigest(hashAlgorithm));
        Calendar cal = Calendar.getInstance();
        byte[] ocsp = null;
        if (chain.length >= 2 && ocspClient != null) {
            ocsp = ocspClient.getEncoded((X509Certificate) chain[0], (X509Certificate) chain[1], null);
        }
        byte[] sh = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp, crlBytes, sigtype);
        byte[] extSignature = externalSignature.sign(sh);
        sgn.setExternalDigest(extSignature, null, externalSignature.getEncryptionAlgorithm());

        byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsaClient, ocsp, crlBytes, sigtype);

        if (estimatedSize + 2 < encodedSig.length)
            throw new IOException("Not enough space");

        byte[] paddedSig = new byte[estimatedSize];
        System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
        sap.close(dic2);
    }
    
    /**
     * Processes a CRL list.
     * @param cert	a Certificate if one of the CrlList implementations needs to retrieve the CRL URL from it.
     * @param crlList	a list of CrlClient implementations
     * @return	a collection of CRL bytes that can be embedded in a PDF.
     */
    public static Collection<byte[]> processCrl(Certificate cert, Collection<CrlClient> crlList) {
        if (crlList == null)
            return null;
        ArrayList<byte[]> crlBytes = new ArrayList<byte[]>();
        for (CrlClient cc : crlList) {
            if (cc == null)
                continue;
            LOGGER.info("Processing " + cc.getClass().getName());
            Collection<byte[]> b = cc.getEncoded((X509Certificate)cert, null);
            if (b == null)
                continue;
            crlBytes.addAll(b);
        }
        if (crlBytes.isEmpty())
            return null;
        else
            return crlBytes;
    }
}
