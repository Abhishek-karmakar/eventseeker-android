package com.freethinking.beats.sdk.data;

public class Review extends BaseJson {
    protected String type;
    protected String content;
    protected String headline;
    protected String rating;
    protected String source;
    protected String author;
    protected ReferenceLink subject;

    public Review() {
        subject = new ReferenceLink();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Review) {
            this.type = ((Review) parseJson).type;
            this.content = ((Review) parseJson).content;
            this.headline = ((Review) parseJson).headline;
            this.rating = ((Review) parseJson).rating;
            this.source = ((Review) parseJson).source;
            this.author = ((Review) parseJson).author;
        } else {
            throw new Exception();
        }
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getHeadline() {
        return headline;
    }

    public String getRating() {
        return rating;
    }

    public String getSource() {
        return source;
    }

    public String getAuthor() {
        return author;
    }

    public ReferenceLink getSubject() {
        return subject;
    }
}
