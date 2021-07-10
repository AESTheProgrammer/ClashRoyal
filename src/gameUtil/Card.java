package gameUtil;

public abstract class Card {
    // cost is the elixir price of the card
    // target is the target type of the card
    // cardImageAddress is address of the image
    // functionality is functionality of this card
    // soundAddress is address of the sound
    // address of the asset of the character
    private int cost;
    private Target target;
    private String cardAddress;
    private Function functionality;
    private String soundAddress;
    private String characterImageAddress;

    /**
     * this is a constructor
     * @param cost is the elixir price of the card
     * @param target is the target type of the card
     * @param cardImageAddress is address of the image
     * @param soundAddress is address of the sound
     * @param functionality is functionality of this card
     * @param characterImageAddress is the address of the asset of this image from root
     */
    public Card(int cost,
                Target target,
                String cardImageAddress,
                String soundAddress,
                Function functionality,
                String characterImageAddress)
    {
        this.characterImageAddress = characterImageAddress;
        this.soundAddress = soundAddress;
        this.functionality = functionality;
        this.cost = cost;
        this.target = target;
        this.cardAddress = cardImageAddress;
    }

    /**
     * this is a getter
     * @return target type of this card
     */
    public Target getTarget() {
        return target;
    }

    /**
     * this method is a getter
     * @return image address of the character
     */
    public String getCharacterImageAddress() {
        return characterImageAddress;
    }

    /**
     * this method is a getter
     * @return type of the card
     */
    public abstract Object getName();

    /**
     * this method is a getter
     * @return hp of the player
     */
    public abstract int getHP();

    /**
     * this method is a getter
     * @return damage of the player
     */
    public abstract int getDamage();

    /**
     * this method is a getter
     * @return sight range of the character
     */
    public abstract double getRange();
}