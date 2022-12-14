package scrapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class WinamaxScrapTools {

	public void fillMatchProperties(String data, Match match) {
		match.setTitle(GlobalScrapTools.getPropertyValue(data, "title", ",", true, true));
		match.setEquipeHome(GlobalScrapTools.getPropertyValue(data, "competitor1Name", ",", true, true));
		match.setEquipeAway(GlobalScrapTools.getPropertyValue(data, "competitor2Name", ",", true, true));
		match.setMainBetId(GlobalScrapTools.getPropertyValue(data, "mainBetId", ",", true, true));
		match.setTournamentId(GlobalScrapTools.getPropertyValue(data, "tournamentId", ",", true, true));
	}

	// web page -> list of matches
	public List<Match> pageToMatches(String data, String tournamentNumber) {
		List<Match> matches = new ArrayList<>();
		data = data.substring(data.indexOf("matches\":{"));
		while (data.contains("matchId")) {
			int index = data.indexOf("matchId");
			Match match = new Match();
			fillMatchProperties(data, match);
			if (match.getTournamentId().equals(tournamentNumber)) {
				matches.add(match);
			}
			data = data.substring(index + 1);
		}
		return matches;
	}

	// list of matches -> list of bets
	public List<Bet> matchesToBets(String data, List<Match> matches) {
		data = data.substring(data.indexOf("\"bets\":{"));
		List<Bet> bets = new ArrayList<>();
		for (Match match : matches) {
			bets.add(new Bet(match, data.indexOf(match.getMainBetId())));
		}
		Collections.sort(bets);
		for (Bet bet : bets) {
			data = data.substring(data.indexOf(bet.getId()));
			String rawStringOutcomes = (GlobalScrapTools.getPropertyValue(data, "outcomes", "]", true, true));
			String[] stringOutcomes = rawStringOutcomes.substring(1).split(",");
			List<BetOutcome> outcomes = new ArrayList<>();
			for (String outcome : stringOutcomes) {
				outcomes.add(new BetOutcome(outcome, bet.getMatch(), bet));
			}
			bet.setOutcomes(outcomes);
		}
		fillOutcomesProperties(data, bets);
		return bets;
	}

	public void fillOutcomesProperties(String data, List<Bet> bets) {
		data = data.substring(data.indexOf("\"outcomes\":{"));
		for (Bet bet : bets) {
			for (BetOutcome outcome : bet.getOutcomes()) {
				String subdata = data.substring(data.indexOf(outcome.getId()));
				outcome.setLabel(GlobalScrapTools.getPropertyValue(subdata, "label", ",", true, true));
				// TODO: label can also be used as the team's "short_name" on Winamax.
				String odd = GlobalScrapTools.getPropertyValue(subdata, outcome.getId(), ",", true, true);
				odd = odd.replace("}", "");
				outcome.setOdd(Double.parseDouble(odd));
			}
		}
	}

	public void winamaxAllInOne(WinamaxWebPages page) throws IOException {
		Document doc = Jsoup.connect(page.getUrl()).get();
		String data = doc.data();
		List<Match> matches = pageToMatches(data, page.getTournamentId());
		List<Bet> bets = matchesToBets(data, matches);
		bets.forEach(b -> System.out.println(b));
	}
}
