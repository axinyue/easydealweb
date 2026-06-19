<template>
  <main class="about-page">
    <section class="about-hero">
      <p class="eyebrow">项目介绍</p>
      <h1>Easy Deal 交易房间</h1>
      <p>
        Easy Deal 面向线下拍卖和隐藏竞价场景，帮助房主快速创建临时交易房间，
        邀请参与者加入，并在同一页面完成商品上架、实时出价、隐藏报价和统一开标。
      </p>
    </section>

    <section class="about-section">
      <h2>核心功能</h2>
      <ul>
        <li>房主创建房间后，系统自动生成房间编号和默认邀请码。</li>
        <li>参与者可通过房间号、邀请码或邀请链接加入房间。</li>
        <li>支持公开拍卖模式，参与者出价后房间内实时同步当前最高价。</li>
        <li>支持隐藏竞价模式，开标前隐藏报价，关闭出价后展示最终结果。</li>
        <li>房主可生成一码一人、一码一房、一码多人等不同类型的邀请码。</li>
        <li>房主可管理成员、停止新用户进入，并在需要时清理整个房间数据。</li>
      </ul>
    </section>

    <section class="about-section">
      <h2>使用流程</h2>
      <ol>
        <li>房主点击“创建房间”，进入自动命名的交易房间。</li>
        <li>房主打开“邀请”，复制邀请码链接发送给线下参与者。</li>
        <li>参与者打开链接或输入邀请码加入房间。</li>
        <li>房主选择拍卖或竞标模式并添加商品。</li>
        <li>参与者输入金额出价，房间状态会通过网络连接实时同步。</li>
        <li>竞标模式下，房主关闭出价后，所有人可查看开标结果。</li>
      </ol>
    </section>

    <section class="about-section">
      <h2>适用场景</h2>
      <p>
        适合小型线下交易会、二手物品竞价、社群内部拍卖、现场隐藏报价收集等场景。
        系统使用临时用户身份，参与门槛低，不需要完整账号注册流程。
      </p>
    </section>

    <section class="about-section english-section">
      <p class="eyebrow">English Guide</p>
      <h2>What Is EasyDeal?</h2>
      <p>
        EasyDeal is a lightweight deal room for in-person auctions and sealed bidding.
        A host can create a room, invite participants with codes or links, publish items,
        collect real-time bids, and reveal hidden bids after closing the bidding stage.
      </p>

      <h2>How To Use It</h2>
      <ol>
        <li>Create a room as the host.</li>
        <li>Share the invite code or invite link with participants at the same offline location.</li>
        <li>Add items and choose either public auction mode or sealed bidding mode.</li>
        <li>Participants join the room and submit bids from their own devices.</li>
        <li>For sealed bidding, close the item bidding to reveal the final result.</li>
      </ol>
    </section>
  </main>
</template>

<style scoped>
.about-page {
  display: grid;
  gap: 18px;
  width: min(960px, calc(100% - 32px));
  margin: 32px auto;
}

.about-hero,
.about-section {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 22px;
  background: var(--color-background-soft);
}

.about-hero {
  display: grid;
  gap: 10px;
}

.eyebrow {
  margin: 0;
  color: #24745c;
  font-size: 12px;
  font-weight: 800;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: var(--color-heading);
  font-size: 32px;
  line-height: 1.15;
}

h2 {
  margin-bottom: 12px;
  color: var(--color-heading);
  font-size: 20px;
}

p,
li {
  line-height: 1.8;
}

ul,
ol {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 20px;
}
</style>
