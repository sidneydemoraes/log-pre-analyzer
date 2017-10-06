package br.com.infoglobo.analysis;

public class LogAnalyzer {

	private static DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("yyyy dd MMM"); //G+ - 2017 06 Jul 12:04:07
	// private static DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");
	// private static DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //mobileauth 2017-09-18 17:31:15

	// private static Pattern myPattern = Pattern.compile(".*\\[ExactTargetWSImpl.*\\]");
	// private static Pattern myPattern = Pattern.compile(".*\\[UsuarioAutorizacaoWebService\\]");
	private static Pattern myPattern = Pattern.compile(".* ERROR \\[[0-9]+\\]");
	// private static Pattern myPattern = Pattern.compile(".* ERROR"); // mobileauth
	private static LocalDate myLocalDate = LocalDate.of(2017, Month.SEPTEMBER, 29);

	public static void main(String args[]) throws URISyntaxException, IOException {

		List<Integer> quantidadeBudegasPorDia = new ArrayList<>();

		// Files.list(Paths.get("C:/Users/wantunes.EG14149/Documents/AC-1074 - EDG API UnknownHostException/"))
		Files.list(Paths.get("C:\\Users\\wantunes.EG14149\\Documents\\logs\\gmais-prd\\021017"))
			// .filter(p -> p.getFileName().toString().startsWith("catalina") || p.getFileName().toString().startsWith("globomais-error"))
			// .filter(p -> p.getFileName().toString().startsWith("edg-api.log"))
			.filter(p -> p.getFileName().toString().startsWith("globomais-error"))
			// .filter(p -> p.getFileName().toString().startsWith("exacttarget-sync"))
			// .filter(p -> p.getFileName().toString().startsWith("edg-api-error.log"))
			// .filter(p -> p.getFileName().toString().startsWith("ecommerce"))
			.peek(System.out::println)
			.forEach(p -> {
				try {
					Map<LocalDate, List<String>> myErrorsByLocalDate = grouypByDate(p);

					myErrorsByLocalDate.keySet().stream()
						.forEach(k -> {
							//					if (k.isAfter(myLocalDate)) {
							System.out.println("----------- " + k);

							Map<String, List<String>> myLinesByErrors = groupByErrors(myErrorsByLocalDate, k);

							// Para obter o total de budegas...
							Integer sum = myLinesByErrors.keySet().stream().mapToInt(exceptionType -> myLinesByErrors.get(exceptionType).size()).sum();
							quantidadeBudegasPorDia.add(sum);
							System.out.println("------- Total de budegas no dia " + k + ": " + sum);

							myLinesByErrors.keySet().stream().forEach(exceptionType -> {
								System.out.println(String.format("Count for ###%s### --> %s", exceptionType, myLinesByErrors.get(exceptionType).size()));

/*							try {
								for (String myLine : myErrorsByLocalDate.get(k)) {
									if (myLine.contains(exceptionType)) {
										Map<String, List<String>> myGroupedByCausedMap = groupByCaused(p, myLine, k, exceptionType);

										myGroupedByCausedMap.keySet().stream()
										.forEach(myKey -> {
											System.out.println(String.format("\t - %s ---- Count: %s", myKey, myGroupedByCausedMap.get(myKey).size()));
										});

										if (!myGroupedByCausedMap.isEmpty())
											break;
									}
								}


							} catch (IOException e) {
								throw new RuntimeException("Something has happened during filter process...", e);
							}*/


							});
							System.out.println();
							System.out.println();
							//					}
						});
				} catch (IOException e) {
					throw new RuntimeException("Something has happened during filter process...", e);
				}
			});

		quantidadeBudegasPorDia.stream().mapToInt(v -> v).average().ifPresent(avg -> {
			System.out.println("Média de budegas que aconteceram em " + quantidadeBudegasPorDia.size() + " dias: " + avg);
		});
	}

	private static Map<String, List<String>> groupByCaused(Path path, String line, LocalDate currentLocalDate, String exceptionType) throws IOException {
		List<String> allLines = Files.readAllLines(path);
		int myIndex = allLines.indexOf(line);
		boolean stopIt = false;

		List<String> myList = new ArrayList<>();

		while (!stopIt) {
			for (int i = myIndex; i < allLines.size(); i++) {
				if (allLines.get(i).contains("Caused by")) {
					myList.add(allLines.get(i));
					myIndex = ++i;
					break;
				}
				try {
					LocalDate myLocalDate = LocalDate.parse(allLines.get(i).substring(0, 6) + " 2017" , myFormat);
					if (!currentLocalDate.equals(myLocalDate)) {
						stopIt = true;
						break;
					}

				} catch (Exception e) { }

				if(i == allLines.size()-1) {
					stopIt = true;
				}
			}
		}

		Map<String, List<String>> myGroupedByCaused = myList.stream().collect(Collectors.groupingBy(t -> t, TreeMap::new, Collectors.toList()));
		return myGroupedByCaused;

	}

	private static Map<LocalDate, List<String>> grouypByDate(Path path) throws IOException {
		return Files.lines(path).filter(l -> {
			try {
				LocalDate myLocalDate = LocalDate.parse("2017 " + l.substring(0, 6), myFormat); // g+
				// LocalDate myLocalDate = LocalDate.parse(l.substring(0, 6) + " 2017" , myFormat);
				// LocalDate myLocalDate = LocalDate.parse(l.substring(0, 19), myFormat); // mobileauth

				// return l.contains(" ERROR ") && myLocalDate.isAfter(CustomLogAnalyser.myLocalDate) && l.contains("[UsuarioAutorizacaoWebService]");
				// return l.contains(" ERROR ") && myLocalDate.isAfter(CustomLogAnalyser.myLocalDate) && l.contains("[ExactTargetWSImpl   ]");
				return l.contains(" ERROR ") && myLocalDate.isAfter(CustomLogAnalyser.myLocalDate);
			} catch (Exception e) {
				return false;
			}
		}).filter(l -> {
			return l.contains("gerar o authToken para o usuário");
		}).peek(System.out::println).collect(Collectors.groupingBy(l -> {
			// return LocalDate.parse(l.substring(0, 6) + " 2017" , myFormat);
			// return LocalDate.parse(l.substring(0, 19), myFormat); // mobileauth
			return LocalDate.parse("2017 " + l.substring(0, 6), myFormat); //g+
		}, TreeMap::new, Collectors.toList()));
	}

	private static Map<String, List<String>> groupByErrors(Map<LocalDate, List<String>> errorsByDate, LocalDate localDate) {
		return errorsByDate.get(localDate).stream().collect(Collectors.groupingBy(s -> {
			Matcher m = myPattern.matcher(s);
			m.find();
			return s.substring(m.end()+1, s.length());
		}, TreeMap::new, Collectors.toList()));
	}
}
