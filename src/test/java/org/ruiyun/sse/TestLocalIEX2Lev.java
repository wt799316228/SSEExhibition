/** * Copyright (C) 2016 Tarik Moataz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//***********************************************************************************************//

// This file contains the step-by-step local benchmarking of the IEX-2Lev. The encrypted data structure remains in the RAM.
// This file also gathers stats useful to give some insights about the scheme implementation
// One needs to wait until the complete creation of the encrypted data structures of IEX-2Lev in order to issue queries.
// Queries need to be in the form of CNF. Follow on-line instructions
//***********************************************************************************************//
package org.ruiyun.sse;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;

public class TestLocalIEX2Lev {

	public static void main(String[] args) throws Exception {

		Printer.addPrinter(new Printer(Printer.LEVEL.EXTRA));

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter your password :");

		String pass = keyRead.readLine();

		List<byte[]> listSK = IEX2Lev.keyGen(256, pass, "salt/salt", 100000);

		long startTime = System.nanoTime();

		BufferedWriter writer = new BufferedWriter(new FileWriter("logs.txt", true));

		System.out.println("Enter the relative path name of the folder that contains the files to make searchable: ");

		String pathName = keyRead.readLine();

		// Creation of different files based on selectivity
		// Selectivity was computed in an inclusive way. All files that include
		// x(i+1) include necessarily xi
		// This is used for benchmarking and can be taken out of the code

		ArrayList<File> listOfFile = new ArrayList<File>();
		TextProc.listf(pathName, listOfFile);

		TextProc.TextProc(false, pathName);

		int bigBlock = 1000;
		int smallBlock = 100;

		long startTime2 = System.nanoTime();

		IEX2Lev disj = IEX2Lev.setup(listSK, TextExtractPar.lp1, TextExtractPar.lp2, bigBlock, smallBlock, 0);

		long endTime2 = System.nanoTime();
		long totalTime2 = endTime2 - startTime2;

		// Writing logs

		Printer.debugln("\n*****************************************************************");
		Printer.debugln("\n\t\tSTATS");
		Printer.debugln("\n*****************************************************************");

		// Printer.debugln("\nNumber of keywords
		// "+TextExtractPar.totalNumberKeywords);
		Printer.debugln("\nNumber of (w, id) pairs " + TextExtractPar.lp2.size());
		writer.write("\n Number of (w, id) pairs " + TextExtractPar.lp2.size());

		Printer.debugln("\nTotal number of stored (w, Id) including in local MM : " + IEX2Lev.numberPairs);
		writer.write("\n Total number of stored (w, Id) including in local MM : " + IEX2Lev.numberPairs);

		Printer.debugln("\nTime elapsed per (w, Id) in ns: " + totalTime2 / IEX2Lev.numberPairs);
		writer.write("\n Time elapsed per (w, Id) in ns: " + totalTime2 / IEX2Lev.numberPairs);

		Printer.debugln("\nTotal Time elapsed for the entire construction in seconds: " + totalTime2 / 1000000000);
		writer.write("\n Total Time elapsed for the entire construction in seconds: " + totalTime2 / 1000000000);

		Printer.debugln("\nRelative Time elapsed per (w, Id) in ns: " + totalTime2 / TextExtractPar.lp1.size());
		writer.write("\n Relative Time elapsed per (w, Id) in ns: " + totalTime2 / TextExtractPar.lp1.size());

		// The two commented commands are used to compute the size of the
		// encrypted Local multi-maps and global multi-maps

		// Printer.debugln("\nSize of the Structure LMM: "+
		// SizeOf.humanReadable(SizeOf.deepSizeOf(disj.getLocalMultiMap())));
		// Printer.debugln("\nSize of the Structure MMg: "+
		// SizeOf.humanReadable(SizeOf.deepSizeOf(disj.getGlobalMM())));
		writer.close();

		while (true) {
			System.out.println("How many disjunctions? ");
			int numDisjunctions = Integer.parseInt(keyRead.readLine());

			// Storing the CNF form
			String[][] bool = new String[numDisjunctions][];
			for (int i = 0; i < numDisjunctions; i++) {
				System.out.println("Enter the keywords of the disjunctions ");
				bool[i] = keyRead.readLine().split(" ");
			}

			test("log-1.txt", "Test", 1, disj, listSK, bool);
		}

	}

	public static void test(String output, String word, int numberIterations, IEX2Lev disj, List<byte[]> listSK,
			String[][] bool) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, IOException {

		long minimum = 1000000000;
		long maximum = 0;
		long average = 0;

		// Generate the IEX token
		List<String> searchBol = new ArrayList<String>();
		for (int i = 0; i < bool[0].length; i++) {
			searchBol.add(bool[0][i]);
		}

		for (int g = 0; g < numberIterations; g++) {

			// Generation of stream file to measure size of the token

			long startTime3 = System.nanoTime();

			// Printer.debugln(searchBol);

			Set<String> tmpBol = IEX2Lev.query(IEX2Lev.token(listSK, searchBol), disj);

			// Printer.debugln(tmpBol);

			for (int i = 1; i < bool.length; i++) {
				Set<String> finalResult = new HashSet<String>();
				for (int k = 0; k < bool[0].length; k++) {
					List<String> searchTMP = new ArrayList<String>();
					searchTMP.add(bool[0][k]);
					for (int r = 0; r < bool[i].length; r++) {
						searchTMP.add(bool[i][r]);
					}

					List<TokenDIS> tokenTMP = IEX2Lev.token(listSK, searchTMP);

					Set<String> result = new HashSet<String>(RR2Lev.query(tokenTMP.get(0).getTokenMMGlobal(),
							disj.getGlobalMM().getDictionary(), disj.getGlobalMM().getArray()));

					if (!(tmpBol.size() == 0)) {
						List<Integer> temp = new ArrayList<Integer>(
								disj.getDictionaryForMM().get(new String(tokenTMP.get(0).getTokenDIC())));

						if (!(temp.size() == 0)) {
							int pos = temp.get(0);

							for (int j = 0; j < tokenTMP.get(0).getTokenMMLocal().size(); j++) {

								Set<String> temporary = new HashSet<String>();
								List<String> tempoList = RR2Lev.query(tokenTMP.get(0).getTokenMMLocal().get(j),
										disj.getLocalMultiMap()[pos].getDictionary(),
										disj.getLocalMultiMap()[pos].getArray());

								if (!(tempoList == null)) {
									temporary = new HashSet<String>(
											RR2Lev.query(tokenTMP.get(0).getTokenMMLocal().get(j),
													disj.getLocalMultiMap()[pos].getDictionary(),
													disj.getLocalMultiMap()[pos].getArray()));
								}

								finalResult.addAll(temporary);

								if (tmpBol.isEmpty()) {
									break;
								}

							}
						}

					}
				}
				tmpBol.retainAll(finalResult);

			}

			System.out.println("Final result " + tmpBol);
			long endTime3 = System.nanoTime();
			long totalTime3 = endTime3 - startTime3;

			if (totalTime3 < minimum) {

				minimum = totalTime3;

			}

			if (totalTime3 > maximum) {

				maximum = totalTime3;

			}

			average = average + totalTime3;

		}

		BufferedWriter writer2 = new BufferedWriter(new FileWriter(output, true));
		writer2.write("\n Word " + word + " minimum " + minimum);
		writer2.write("\n Word " + word + " maximum " + maximum);
		writer2.write("\n Word " + word + " average " + average / numberIterations + "\n\n");
		writer2.close();

	}
}
