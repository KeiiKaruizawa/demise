package gr.larryngton.demise.features.command;

import gr.larryngton.demise.Demise;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatSuggestionHandler
{
    public enum SuggestionType { CLIENT, SERVER }

    public static class Suggestion
    {
        @Getter private final int replaceStart;
        @Getter private final String insertText;
        @Getter private final String displayText;
        @Getter private final SuggestionType type;

        public Suggestion(int replaceStart, String insertText, String displayText, SuggestionType type)
        {
            this.replaceStart = replaceStart;
            this.insertText = insertText;
            this.displayText = displayText;
            this.type = type;
        }
    }

    public interface ServerCompletionRequester
    {
        void request(String textBeforeCursor);
    }

    private static final String CLIENT_PREFIX = ".";
    private static final long SERVER_DEBOUNCE_MS = 150L;
    private static final int MAX_VISIBLE_SUGGESTIONS = 6;

    private final ServerCompletionRequester requester;

    private List<Suggestion> suggestions = Collections.emptyList();
    private int suggestionIndex = 0;
    private int scrollOffset = 0;
    private String lastInput = "";

    private boolean serverRequestDirty = false;
    private String pendingServerInput = "";
    private long lastServerRequestTime = 0L;

    public ChatSuggestionHandler(ServerCompletionRequester requester)
    {
        this.requester = requester;
    }

    public void onInputChanged(String fullText, int cursorPos)
    {
        if (fullText.equals(this.lastInput))
        {
            return;
        }

        this.lastInput = fullText;
        this.suggestionIndex = 0;
        this.scrollOffset = 0;

        if (fullText.startsWith(CLIENT_PREFIX))
        {
            this.suggestions = buildClientSuggestions(fullText);
            this.serverRequestDirty = false;
        }
        else if (fullText.startsWith("/"))
        {
            pruneStaleSuggestions(fullText);
            this.pendingServerInput = fullText.substring(0, cursorPos);
            this.serverRequestDirty = true;
        }
        else
        {
            this.suggestions = Collections.emptyList();
            this.serverRequestDirty = false;
        }
    }

    public void tick()
    {
        if (this.serverRequestDirty && System.currentTimeMillis() - this.lastServerRequestTime >= SERVER_DEBOUNCE_MS)
        {
            this.serverRequestDirty = false;
            this.lastServerRequestTime = System.currentTimeMillis();

            if (this.requester != null)
            {
                this.requester.request(this.pendingServerInput);
            }
        }
    }

    public void onServerCompletionsReceived(String currentFullText, String[] completions)
    {
        if (!currentFullText.startsWith("/")) {
            return;
        }

        int replaceStart = findLastTokenStart(currentFullText);
        List<Suggestion> result = new ArrayList<>();

        for (String completion : completions)
        {
            if (completion != null && completion.length() > 0)
            {
                result.add(new Suggestion(replaceStart, completion, completion, SuggestionType.SERVER));
            }
        }

        this.suggestions = result;
        if (this.suggestionIndex >= result.size())
        {
            this.suggestionIndex = 0;
            this.scrollOffset = 0;
        }
    }

    public boolean hasSuggestions()
    {
        return !this.suggestions.isEmpty();
    }

    public List<Suggestion> getSuggestions()
    {
        int from = Math.min(this.scrollOffset, this.suggestions.size());
        int to = Math.min(from + MAX_VISIBLE_SUGGESTIONS, this.suggestions.size());
        return Collections.unmodifiableList(this.suggestions.subList(from, to));
    }

    public int getTotalSuggestionCount()
    {
        return this.suggestions.size();
    }

    public int getSelectedIndex()
    {
        return this.suggestionIndex;
    }

    public int getSelectedVisibleIndex()
    {
        return this.suggestionIndex - this.scrollOffset;
    }

    public Suggestion getSelected()
    {
        if (this.suggestions.isEmpty())
        {
            return null;
        }

        return this.suggestions.get(this.suggestionIndex);
    }

    public void navigate(int direction)
    {
        if (this.suggestions.isEmpty())
        {
            return;
        }

        int size = this.suggestions.size();
        this.suggestionIndex = ((this.suggestionIndex + direction) % size + size) % size;

        if (this.suggestionIndex < this.scrollOffset)
        {
            this.scrollOffset = this.suggestionIndex;
        }
        else if (this.suggestionIndex >= this.scrollOffset + MAX_VISIBLE_SUGGESTIONS)
        {
            this.scrollOffset = this.suggestionIndex - MAX_VISIBLE_SUGGESTIONS + 1;
        }

        if (this.suggestionIndex == 0)
        {
            this.scrollOffset = 0;
        }
        else if (this.suggestionIndex == size - 1)
        {
            this.scrollOffset = Math.max(0, size - MAX_VISIBLE_SUGGESTIONS);
        }
    }

    public boolean handleSend(String fullText)
    {
        boolean handled = Demise.INSTANCE.getCommandManager()
                .handle(fullText);

        if (handled)
        {
            reset();
        }

        return handled;
    }

    public void reset()
    {
        this.suggestions = Collections.emptyList();
        this.suggestionIndex = 0;
        this.scrollOffset = 0;
        this.lastInput = "";
        this.serverRequestDirty = false;
    }

    private List<Suggestion> buildClientSuggestions(String fullText)
    {
        String withoutPrefix = fullText.substring(CLIENT_PREFIX.length());
        int firstSpaceIdx = withoutPrefix.indexOf(' ');
        List<Suggestion> result = new ArrayList<>();

        if (firstSpaceIdx == -1)
        {
            String partial = withoutPrefix;
            int replaceStart = CLIENT_PREFIX.length();

            for (Command cmd : Demise.INSTANCE.getCommandManager().cmd)
            {
                if (cmd.getAliases() == null) continue;

                for (String alias : cmd.getAliases())
                {
                    if (alias != null && alias.toLowerCase().startsWith(partial.toLowerCase()))
                    {
                        String display = CLIENT_PREFIX + alias + " - " + cmd.getUsage();
                        result.add(new Suggestion(replaceStart, alias, display, SuggestionType.CLIENT));
                    }
                }
            }
        }
        else
        {
            String commandName = withoutPrefix.substring(0, firstSpaceIdx);
            Command match = null;

            for (Command cmd : Demise.INSTANCE.getCommandManager().cmd)
            {
                if (cmd.getAliases() == null) continue;

                for (String alias : cmd.getAliases())
                {
                    if (alias != null && alias.equalsIgnoreCase(commandName))
                    {
                        match = cmd;
                        break;
                    }
                }
                if (match != null) break;
            }

            if (match != null)
            {
                String argsText = withoutPrefix.substring(firstSpaceIdx + 1);
                String[] args = argsText.trim().isEmpty() ? new String[0] : argsText.trim().split("\\s+");

                if (argsText.isEmpty() || argsText.endsWith(" "))
                {
                    String[] extended = new String[args.length + 1];
                    System.arraycopy(args, 0, extended, 0, args.length);
                    extended[args.length] = "";
                    args = extended;
                }

                String lastArg = args.length == 0 ? "" : args[args.length - 1];
                int replaceStart = fullText.length() - lastArg.length();

                for (String suggestion : match.getArgumentSuggestions(args))
                {
                    if (suggestion != null && suggestion.toLowerCase().startsWith(lastArg.toLowerCase()))
                    {
                        result.add(new Suggestion(replaceStart, suggestion, suggestion, SuggestionType.CLIENT));
                    }
                }

            }
        }

        return result;
    }

    private void pruneStaleSuggestions(String currentText)
    {
        if (this.suggestions.isEmpty())
        {
            return;
        }

        List<Suggestion> valid = new ArrayList<>();

        for (Suggestion suggestion : this.suggestions)
        {
            if (suggestion.getReplaceStart() >= 0 && suggestion.getReplaceStart() <= currentText.length())
            {
                valid.add(suggestion);
            }
        }

        this.suggestions = valid;

        if (this.suggestionIndex >= valid.size())
        {
            this.suggestionIndex = 0;
            this.scrollOffset = 0;
        }
    }

    private int findLastTokenStart(String text)
    {
        int idx = text.length();

        while (idx > 0 && !Character.isWhitespace(text.charAt(idx - 1)))
        {
            idx--;
        }

        return idx;
    }
}