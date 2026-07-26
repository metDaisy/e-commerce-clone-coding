import React from "react";
import HeroBanner from "../../components/HeroBanner";
import ProductGrid from "../../components/ProductGrid";
import ScrollRow from "../../components/ScrollRow";
import {
  bestSellers,
  categoryCards,
  categoryCards2,
  categoryCards3,
  dealsCards,
} from "../../data/products";
import "./Home.css";

const Home: React.FC = () => {
  return (
    <>
      <HeroBanner />
      <ProductGrid cards={categoryCards} />
      <ScrollRow section={bestSellers[0]} />
      <ProductGrid cards={categoryCards2} />
      <ScrollRow section={bestSellers[1]} />
      <div className="promo-banner" id="promo-banner">
        <div className="promo-banner__inner">
          <h3 className="promo-banner__title">
            Top picks for Republic of Korea
          </h3>
          <p className="promo-banner__subtitle">
            Discover trending products curated just for you
          </p>
        </div>
      </div>
      <ProductGrid cards={categoryCards3} />
      <ScrollRow section={bestSellers[2]} />
      <ProductGrid cards={dealsCards} />
    </>
  );
};

export default Home;
